package quire.app.ttsservice.synthesis

import quire.index.MatchResult
import quire.tts.casting.Cast
import quire.tts.engine.BoundaryKind
import quire.tts.engine.TtsEngine

/**
 * Turns one incoming chunk into a single continuous utterance, voiced per [Segmenter]'s
 * plan — the mechanic that makes multi-voice work through Android's one-call,
 * one-utterance, one-voice `TextToSpeechService` contract (QUI-024).
 *
 * One [TtsEngine], called once per segment, strictly in order: "one ONNX session,
 * serialised inference" is true by construction, not by a lock, since nothing here starts
 * a second synthesis before the first returns.
 */
class UtteranceSynthesizer(private val engine: TtsEngine) {

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

        for (segment in Segmenter.plan(chunk, match, cast)) {
            if (segment.text.isEmpty() || cancelled()) return@sequence

            val raw = engine.synthesize(segment.text, segment.voiceId, cancelled) ?: return@sequence
            if (cancelled()) return@sequence

            val factor = hostRate * segment.rate
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
                yield(AudioEvent.Range(boundary.charStart + segment.offset, boundary.charEnd + segment.offset))
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
}
