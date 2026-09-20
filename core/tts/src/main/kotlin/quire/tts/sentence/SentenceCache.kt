package quire.tts.sentence

import quire.model.IndexEntry
import quire.tts.casting.Cast
import quire.tts.engine.Boundary
import quire.tts.engine.BoundaryKind
import quire.tts.engine.TtsChunk
import quire.tts.engine.TtsEngine

/**
 * One voiced run of a cached sentence's synthesis.
 *
 * [start]/[end] are raw offsets into the entry's own [IndexEntry.text] — this is finer than
 * the whole sentence because a sentence mixing narration and dialogue must still be
 * synthesised per voice (QUI-024), so the unit cached is the span, not always the whole
 * sentence. [chunk] is null when the engine failed or was cancelled mid-span; callers must
 * treat that as a miss rather than serve a hole.
 */
data class CachedSpan(
    val start: Int,
    val end: Int,
    val speakerId: String?,
    val voiceId: Int,
    val rate: Double,
    val chunk: TtsChunk?,
)

/** One [IndexEntry]'s full synthesis, one [CachedSpan] per voice run. */
data class CachedSentence(val seq: Int, val spans: List<CachedSpan>)

/**
 * The audio and boundaries [chunk] carries for [offset] characters into whatever range was
 * requested — the read side of [CachedSentence.fragment]. [chunk]'s own boundaries are
 * already rebased to start at 0 for the requested range, mirroring what a fresh
 * [TtsEngine.synthesize] call over just that text would have produced.
 */
data class SentenceFragment(
    val offset: Int,
    val speakerId: String?,
    val voiceId: Int,
    val rate: Double,
    val chunk: TtsChunk,
)

/**
 * Synthesises a whole [IndexEntry] once, keyed by [IndexEntry.seq], so a host that chunks
 * by clause (measured on NeoReader: 42 of 73 chunks ended on a comma) gets every clause of
 * one sentence served from a single continuous synthesis instead of one standalone
 * utterance per clause — the fix for QUI-030's sentence-final-intonation-on-a-fragment bug.
 *
 * Not thread-safe, same as [quire.index.Matcher]: one cache per TTS session.
 */
class SentenceCache(private val engine: TtsEngine) {

    private val bySeq = LinkedHashMap<Int, CachedSentence>()

    /** The full synthesis of [entry]: built once and reused for every later clause of it. */
    fun sentence(entry: IndexEntry, cast: Cast, cancelled: () -> Boolean): CachedSentence =
        bySeq.getOrPut(entry.seq) { build(entry, cast, cancelled) }

    /** The entries currently held, for tests and memory accounting. */
    val cachedSeqs: Set<Int> get() = bySeq.keys

    /**
     * Drops every cached sentence the reader has moved past — the cache must not grow
     * without bound as a chapter plays. Safe to call with a seq the cache never held.
     */
    fun evictBefore(seq: Int) {
        bySeq.keys.retainAll { it >= seq }
    }

    private fun build(entry: IndexEntry, cast: Cast, cancelled: () -> Boolean): CachedSentence {
        val spans = entry.spans
            .filter { it.start < it.end }
            .sortedBy { it.start }
            .map { span ->
                val assignment = span.speakerId?.let { cast.voices[it] }
                val voiceId = assignment?.voiceId ?: cast.narratorVoiceId
                val rate = assignment?.rate ?: 1.0
                val text = entry.text.substring(span.start, span.end)
                val chunk = if (cancelled()) null else engine.synthesize(text, voiceId, cancelled)
                CachedSpan(span.start, span.end, span.speakerId, voiceId, rate, chunk)
            }
        return CachedSentence(entry.seq, spans)
    }
}

/**
 * The cached audio covering raw offsets [start] until [end] of this sentence's text, split
 * back into per-voice fragments in order.
 *
 * Returns null when any overlapping span never finished synthesising, so the caller falls
 * back to synthesising the fragment fresh (today's behaviour) rather than serve a hole —
 * "a cache miss must never stall".
 */
fun CachedSentence.fragment(start: Int, end: Int): List<SentenceFragment>? {
    val out = mutableListOf<SentenceFragment>()
    for (span in spans) {
        val s = maxOf(span.start, start)
        val e = minOf(span.end, end)
        if (e <= s) continue
        val chunk = span.chunk ?: return null
        val sliced = sliceChunk(chunk, s - span.start, e - span.start) ?: continue
        out += SentenceFragment(s - start, span.speakerId, span.voiceId, span.rate, sliced)
    }
    return out
}

/**
 * The samples and word boundaries of [chunk] that fall within [start] until [end] of the
 * text it was synthesised from, rebased so the result reads exactly as if [chunk] had been
 * synthesised from that substring alone.
 *
 * Only [BoundaryKind.WORD] boundaries anchor the slice — the same simplification
 * [quire.app.ttsservice.synthesis.UtteranceSynthesizer] already makes when tiling PCM by
 * word — so a sub-range with no whole word inside it (e.g. a lone space) slices to nothing
 * rather than to some approximation.
 */
fun sliceChunk(chunk: TtsChunk, start: Int, end: Int): TtsChunk? {
    val words = chunk.boundaries.filter {
        it.kind == BoundaryKind.WORD && it.charStart >= start && it.charEnd <= end
    }
    if (words.isEmpty()) return null

    val startMs = words.first().startMs
    val endMs = words.last().endMs
    val startSample = (startMs * chunk.sampleRate / 1000).toInt().coerceIn(0, chunk.pcm.size)
    val endSample = (endMs * chunk.sampleRate / 1000).toInt().coerceIn(startSample, chunk.pcm.size)

    val rebased = words.map {
        Boundary(
            startMs = it.startMs - startMs,
            endMs = it.endMs - startMs,
            charStart = it.charStart - start,
            charEnd = it.charEnd - start,
            kind = BoundaryKind.WORD,
        )
    }
    return chunk.copy(pcm = chunk.pcm.copyOfRange(startSample, endSample), boundaries = rebased)
}
