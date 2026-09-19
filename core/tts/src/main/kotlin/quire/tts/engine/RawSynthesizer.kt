package quire.tts.engine

/** What a raw synthesis call hands back: samples and the rate they were generated at. */
data class RawAudio(val pcm: FloatArray, val sampleRate: Int)

/**
 * The native ONNX call [TtsEngine] wraps — sherpa-onnx's `OfflineTts.generate()` in
 * production (ADR-0002: Piper `libritts_r` medium), loaded from a model directory the way
 * `spike/ttsbinding`'s `TtsEngine.load()` already does.
 *
 * That loader needs a `Context` and sherpa-onnx's Android AAR, so it lives in `app:ttsservice`
 * rather than here: `core:tts` is pure Kotlin/JVM by the root build's design (CLAUDE.md §9,
 * "put the logic where it can be tested"), and there is no desktop build of sherpa-onnx's
 * Kotlin binding to test against. This interface is the seam — everything this ticket owns
 * (cancellation, chunk assembly, boundary estimation) is exercised against a fake here and
 * against the real engine once `app:ttsservice` implements it.
 *
 * [cancelled] is polled during generation, not just before it starts — sherpa-onnx's
 * `generate` accepts a per-sample-chunk callback for exactly this, so a real implementation
 * can stop mid-utterance rather than only refusing to begin one.
 */
interface RawSynthesizer {
    /** Returns `null` if [cancelled] became true before synthesis produced any audio. */
    fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio?

    /** Releases the underlying engine session. Idempotent. */
    fun release()
}
