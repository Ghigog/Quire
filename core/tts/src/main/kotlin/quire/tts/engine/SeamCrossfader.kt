package quire.tts.engine

import kotlin.math.pow
import kotlin.random.Random

/**
 * Cross-fades the boundary between two buffers with low-level comfort noise (ADR-0010,
 * QUI-042) — applied only where audio actually crosses from the local engine to a cloud
 * voice or back, never as a continuous floor under the whole book. A hard cut at that one
 * seam is audible where a hard cut between two local sentences is not, because the two
 * sources' noise floors differ.
 */
object SeamCrossfader {

    /**
     * Fades the tail of [a] into the head of [b] over [durationMs], adding comfort noise at
     * [noiseDbfs] across the overlap. Requires the same sample rate; a real caller resamples
     * before this runs, same as anywhere else two buffers meet.
     */
    fun crossfade(
        a: RawAudio,
        b: RawAudio,
        durationMs: Long = 40,
        noiseDbfs: Double = -50.0,
        random: Random = Random.Default,
    ): RawAudio {
        require(a.sampleRate == b.sampleRate) { "cannot cross-fade buffers at different sample rates" }
        val sampleRate = a.sampleRate
        val overlap = ((durationMs * sampleRate) / 1000L).toInt()
            .coerceAtMost(a.pcm.size)
            .coerceAtMost(b.pcm.size)
        if (overlap <= 0) return RawAudio(a.pcm + b.pcm, sampleRate)

        val noiseAmplitude = 10.0.pow(noiseDbfs / 20.0).toFloat()
        val out = FloatArray(a.pcm.size + b.pcm.size - overlap)
        a.pcm.copyInto(out, 0, 0, a.pcm.size - overlap)

        val aTailStart = a.pcm.size - overlap
        for (i in 0 until overlap) {
            val t = (i + 1).toFloat() / (overlap + 1) // 0 (start) .. 1 (end), exclusive of both
            val fadeOut = 1f - t
            val fadeIn = t
            val noise = (random.nextFloat() * 2f - 1f) * noiseAmplitude
            out[aTailStart + i] = a.pcm[aTailStart + i] * fadeOut + b.pcm[i] * fadeIn + noise
        }
        b.pcm.copyInto(out, a.pcm.size, overlap, b.pcm.size)
        return RawAudio(out, sampleRate)
    }
}
