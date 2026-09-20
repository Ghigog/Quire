package quire.tts.engine

import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class LoudnessNormalizerTest {

    private fun tone(amplitude: Float, count: Int = 4_800): RawAudio {
        val pcm = FloatArray(count) { i -> amplitude * sin(i * 0.05).toFloat() }
        return RawAudio(pcm, sampleRate = 24_000)
    }

    @Test
    fun `a quiet buffer is brought up to the target level`() {
        val quiet = tone(amplitude = 0.05f)

        val normalized = LoudnessNormalizer.normalize(quiet, targetDbfs = -17.0)

        assertTrue(LoudnessNormalizer.measureDbfs(normalized) > LoudnessNormalizer.measureDbfs(quiet))
        assertApprox(-17.0, LoudnessNormalizer.measureDbfs(normalized))
    }

    @Test
    fun `a loud buffer is brought down to the target level`() {
        val loud = tone(amplitude = 0.95f)

        val normalized = LoudnessNormalizer.normalize(loud, targetDbfs = -17.0)

        assertTrue(LoudnessNormalizer.measureDbfs(normalized) < LoudnessNormalizer.measureDbfs(loud))
        assertApprox(-17.0, LoudnessNormalizer.measureDbfs(normalized))
    }

    @Test
    fun `normalized samples never clip beyond full scale`() {
        val loud = tone(amplitude = 1.0f)

        val normalized = LoudnessNormalizer.normalize(loud, targetDbfs = -3.0)

        assertTrue(normalized.pcm.all { it in -1f..1f })
    }

    @Test
    fun `a silent buffer is left alone rather than amplifying noise`() {
        val silence = RawAudio(FloatArray(1_000), sampleRate = 24_000)

        val normalized = LoudnessNormalizer.normalize(silence)

        assertTrue(normalized.pcm.all { it == 0f })
    }

    private fun assertApprox(expected: Double, actual: Double, tolerance: Double = 0.1) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected within $tolerance of $actual")
    }
}
