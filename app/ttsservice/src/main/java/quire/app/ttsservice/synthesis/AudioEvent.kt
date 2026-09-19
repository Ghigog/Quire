package quire.app.ttsservice.synthesis

/**
 * One step of the single continuous utterance [UtteranceSynthesizer] produces.
 *
 * A faithful, framework-free mirror of the `SynthesisCallback` contract — exactly one
 * [Started] and, unless cancelled, exactly one [Done], with any number of [Range]/[Audio]
 * pairs between them in playback order. Kept separate from `android.speech.tts` so the
 * synthesis logic is a plain JVM unit test; [CallbackAdapter] is the only class that
 * translates this into real callback calls.
 */
sealed interface AudioEvent {
    data class Started(val sampleRate: Int) : AudioEvent

    /** Offsets are into the original chunk string, end-exclusive — ready for `rangeStart`. */
    data class Range(val charStart: Int, val charEnd: Int) : AudioEvent

    /** Little-endian 16-bit PCM, already sized for [android.speech.tts.SynthesisCallback.audioAvailable]. */
    data class Audio(val pcm: ByteArray) : AudioEvent

    data object Done : AudioEvent
}
