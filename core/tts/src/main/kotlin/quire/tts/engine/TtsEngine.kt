package quire.tts.engine

/**
 * Text plus a voice in, a [TtsChunk] out — the seam CLAUDE.md §2.3 names as one of the four
 * to land early. Wraps a [RawSynthesizer] (production: sherpa-onnx via `app:ttsservice`,
 * ADR-0002) with the two things that are the same whichever engine answers: turning its
 * samples into boundary timestamps ([BoundaryEstimator]) and honouring cancellation.
 */
class TtsEngine(private val synth: RawSynthesizer) {

    /**
     * Synthesises [text] in [voiceId]'s voice. [cancelled] is polled by [synth] during
     * generation as well as here, so a cancellation mid-utterance returns `null` promptly
     * rather than after a whole chunk has been produced and discarded.
     */
    fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean = { false }): TtsChunk? {
        if (cancelled()) return null
        val raw = synth.synthesize(text, voiceId, cancelled) ?: return null
        if (cancelled()) return null

        val durationMs = raw.pcm.size.toLong() * 1000L / raw.sampleRate
        val boundaries = BoundaryEstimator.estimate(text, durationMs)
        return TtsChunk(raw.pcm, raw.sampleRate, voiceId, boundaries)
    }

    /** Releases the underlying engine session. Idempotent. */
    fun release() = synth.release()
}
