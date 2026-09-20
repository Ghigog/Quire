package quire.tts.engine

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Brings a buffer's level to a target so the cast never sounds louder than the narrator
 * (ADR-0010's acoustic requirement, QUI-042) — the local engine and every cloud provider go
 * through this before the ring buffer sees them.
 *
 * This is an RMS-based approximation of the −16 to −18 LUFS mobile-audiobook target, not a
 * full ITU-R BS.1770 loudness meter: no K-weighting, no gating of silent passages. Full LUFS
 * needs both and this ticket did not budget for them; revisit if a listen finds the gap
 * audible.
 */
object LoudnessNormalizer {
    private const val SILENT_RMS = 1e-9

    /** [targetDbfs] is dBFS relative to full scale, e.g. -17.0 for the mid-point of the target band. */
    fun normalize(audio: RawAudio, targetDbfs: Double = -17.0): RawAudio {
        val rms = rms(audio.pcm)
        if (rms < SILENT_RMS) return audio // nothing to scale a near-silent buffer against

        val targetRms = dbfsToLinear(targetDbfs)
        val gain = (targetRms / rms).toFloat()
        val scaled = FloatArray(audio.pcm.size) { i -> (audio.pcm[i] * gain).coerceIn(-1f, 1f) }
        return audio.copy(pcm = scaled)
    }

    private fun rms(pcm: FloatArray): Double {
        if (pcm.isEmpty()) return 0.0
        var sumSquares = 0.0
        for (sample in pcm) sumSquares += sample.toDouble() * sample.toDouble()
        return sqrt(sumSquares / pcm.size)
    }

    private fun dbfsToLinear(dbfs: Double): Double = Math.pow(10.0, dbfs / 20.0)

    /** For tests and diagnostics: the level [audio] is actually at, in dBFS. */
    fun measureDbfs(audio: RawAudio): Double {
        val level = rms(audio.pcm)
        return if (level < SILENT_RMS) Double.NEGATIVE_INFINITY else 20.0 * log10(level)
    }
}
