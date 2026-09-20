package quire.app.ttsservice.synthesis

import quire.index.MatchResult
import quire.tts.casting.Cast
import quire.tts.engine.BoundaryKind
import quire.tts.engine.TtsChunk
import quire.tts.engine.TtsEngine
import quire.tts.sentence.SentenceCache
import quire.tts.sentence.fragment

/**
 * Turns one incoming chunk into a single continuous utterance, voiced per [Segmenter]'s
 * plan — the mechanic that makes multi-voice work through Android's one-call,
 * one-utterance, one-voice `TextToSpeechService` contract (QUI-024).
 *
 * One [TtsEngine], called once per segment, strictly in order: "one ONNX session,
 * serialised inference" is true by construction, not by a lock, since nothing here starts
 * a second synthesis before the first returns.
 *
 * When [sentenceCache] is given, a chunk that is a literal fragment of exactly one matched
 * [quire.model.IndexEntry] is served from that entry's whole-sentence synthesis instead of
 * being resynthesised alone (QUI-030) — the fix for a comma landing like a full stop, since
 * every engine tested gives a standalone utterance sentence-final intonation. Anything the
 * cache cannot serve — a chunk the matcher glued across several entries, or a span whose
 * synthesis failed — falls back to [Segmenter]'s per-chunk plan, which is today's behaviour.
 */
class UtteranceSynthesizer(
    private val engine: TtsEngine,
    private val sentenceCache: SentenceCache? = null,
) {

    /** One segment's audio, however it was produced. */
    private sealed interface Source {
        val offset: Int
        val rate: Double

        data class Fresh(val text: String, override val offset: Int, val voiceId: Int, override val rate: Double) :
            Source

        data class Cached(val chunk: TtsChunk, override val offset: Int, override val rate: Double) : Source
    }

    /**
     * @param hostRate the reader's requested speech rate as a multiplier (1.0 = normal),
     *   already decoded from `SynthesisRequest.speechRate`'s integer percentage. A
     *   character's own [quire.tts.casting.VoiceAssignment.rate] applies on top of this,
     *   never instead of it (Requirements).
     * @param cancelled polled between segments and between words, so a flag flipped from
     *   `onStop` is honoured promptly and never after audio for a further word is queued.
     */
    fun synthesize(
        chunk: String,
        match: MatchResult,
        cast: Cast,
        hostRate: Double,
        cancelled: () -> Boolean,
    ): Sequence<AudioEvent> = sequence {
        var started = false

        val sources = cachedSources(chunk, match, cast, cancelled)
            ?: Segmenter.plan(chunk, match, cast).map { Source.Fresh(it.text, it.offset, it.voiceId, it.rate) }

        if (sentenceCache != null && match.matched) {
            sentenceCache.evictBefore(match.entries.minOf { it.seq })
        }

        for (source in sources) {
            if (cancelled()) return@sequence
            if (source is Source.Fresh && source.text.isEmpty()) return@sequence

            val raw = when (source) {
                is Source.Fresh -> engine.synthesize(source.text, source.voiceId, cancelled) ?: return@sequence
                is Source.Cached -> source.chunk
            }
            if (cancelled()) return@sequence

            val factor = hostRate * source.rate
            val pcm = Resampler.stretch(raw.pcm, factor)
            val words = raw.boundaries
                .filter { it.kind == BoundaryKind.WORD }
                .map { Resampler.scale(it, factor) }

            if (!started) {
                yield(AudioEvent.Started(raw.sampleRate))
                started = true
            }

            // Word boundaries tile [0, duration] with no gaps (BoundaryEstimator gives every
            // word the whole of its sentence's share), so walking them in order and slicing
            // the sample range each one ends at accounts for every sample exactly once.
            var sampleCursor = 0
            for (boundary in words) {
                if (cancelled()) return@sequence
                yield(AudioEvent.Range(boundary.charStart + source.offset, boundary.charEnd + source.offset))
                val endSample = (boundary.endMs * raw.sampleRate / 1000)
                    .toInt().coerceIn(sampleCursor, pcm.size)
                if (endSample > sampleCursor) {
                    yield(AudioEvent.Audio(Pcm16.encode(pcm.copyOfRange(sampleCursor, endSample))))
                }
                sampleCursor = endSample
            }
            // No word boundaries at all (e.g. an all-punctuation span) still has audio to
            // play; whatever a boundary loop didn't cover is flushed here.
            if (sampleCursor < pcm.size) {
                if (cancelled()) return@sequence
                yield(AudioEvent.Audio(Pcm16.encode(pcm.copyOfRange(sampleCursor, pcm.size))))
            }
        }

        if (started) yield(AudioEvent.Done)
    }

    /**
     * The cache's answer for [chunk], or null to fall back to [Segmenter]'s per-chunk plan.
     *
     * The lookup is a literal raw-text search rather than a normalised one: [chunk] is the
     * host's own text and [quire.model.IndexEntry.text] is the same source text the index
     * was built from, so when the chunk is a clause of that one entry it appears in it
     * byte-for-byte, and matching on it directly needs none of [quire.index.Matcher]'s
     * normalisation-aware offset bridging (`OffsetMap`'s quote reach-back among it) —
     * that machinery exists to place spans in the index, not to relocate a chunk that has
     * already been placed. A chunk the matcher glued across several entries, or a chunk
     * whose literal text this search cannot find (a host that alters whitespace), returns
     * null and gets the fresh-synthesis path unchanged.
     */
    private fun cachedSources(
        chunk: String,
        match: MatchResult,
        cast: Cast,
        cancelled: () -> Boolean,
    ): List<Source>? {
        val cache = sentenceCache ?: return null
        if (!match.matched || match.entries.size != 1) return null
        val entry = match.entries.single()
        val start = entry.text.indexOf(chunk)
        if (start < 0) return null

        val sentence = cache.sentence(entry, cast, cancelled)
        val fragments = sentence.fragment(start, start + chunk.length) ?: return null
        if (fragments.isEmpty()) return null
        return fragments.map { Source.Cached(it.chunk, it.offset, it.rate) }
    }
}
