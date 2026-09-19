package quire.app.ttsservice.synthesis

import quire.tts.engine.Boundary

/**
 * Changes an utterance's playback speed by [factor] without a rate knob on the synthesis
 * engine itself.
 *
 * [quire.tts.engine.RawSynthesizer] has none — sherpa-onnx's Piper voices don't expose one
 * ([quire.tts.engine.RawSynthesizer]'s KDoc) — so honouring the host's requested rate, and
 * a character's own rate offset on top of it (`Cast`'s collision-avoidance in
 * [quire.tts.casting.Caster]), both have to happen here, as a post-synthesis resample.
 * Linear interpolation shifts pitch along with speed; that is a fair trade for two
 * characters who would otherwise share a voice exactly (Caster's own stated fallback), and
 * it costs nothing against the RTF budget since it runs on already-generated audio.
 */
object Resampler {

    fun stretch(pcm: FloatArray, factor: Double): FloatArray {
        require(factor > 0.0) { "rate factor must be positive, was $factor" }
        if (factor == 1.0 || pcm.isEmpty()) return pcm

        val outLen = (pcm.size / factor).toInt().coerceAtLeast(1)
        return FloatArray(outLen) { i ->
            val srcPos = i * factor
            val i0 = srcPos.toInt().coerceIn(0, pcm.size - 1)
            val i1 = (i0 + 1).coerceAtMost(pcm.size - 1)
            val frac = (srcPos - i0).toFloat()
            pcm[i0] + (pcm[i1] - pcm[i0]) * frac
        }
    }

    /** Scales a boundary's timing by [factor] — text offsets ([Boundary.charStart]/[charEnd]) are untouched. */
    fun scale(boundary: Boundary, factor: Double): Boundary =
        if (factor == 1.0) boundary else boundary.copy(
            startMs = (boundary.startMs / factor).toLong(),
            endMs = (boundary.endMs / factor).toLong(),
        )
}
