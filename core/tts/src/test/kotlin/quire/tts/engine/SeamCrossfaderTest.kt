package quire.tts.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SeamCrossfaderTest {

    @Test
    fun `total length is the sum of both buffers minus the overlap`() {
        val a = RawAudio(FloatArray(2_400), sampleRate = 24_000) // 100 ms
        val b = RawAudio(FloatArray(2_400), sampleRate = 24_000)

        val out = SeamCrossfader.crossfade(a, b, durationMs = 40)

        val expectedOverlap = (40 * 24_000) / 1000
        assertEquals(a.pcm.size + b.pcm.size - expectedOverlap, out.pcm.size)
    }

    @Test
    fun `the far ends of each buffer are untouched by the fade`() {
        val a = RawAudio(FloatArray(2_400) { 0.3f }, sampleRate = 24_000)
        val b = RawAudio(FloatArray(2_400) { 0.6f }, sampleRate = 24_000)

        val out = SeamCrossfader.crossfade(a, b, durationMs = 40, noiseDbfs = -120.0)

        assertEquals(0.3f, out.pcm.first())
        assertEquals(0.6f, out.pcm.last())
    }

    @Test
    fun `the seam blends toward the incoming buffer, not a hard cut`() {
        val a = RawAudio(FloatArray(2_400) { 0.0f }, sampleRate = 24_000)
        val b = RawAudio(FloatArray(2_400) { 1.0f }, sampleRate = 24_000)

        val out = SeamCrossfader.crossfade(a, b, durationMs = 40, noiseDbfs = -120.0, random = Random(0))

        val overlapStart = a.pcm.size - (40 * 24_000) / 1000
        val overlapEnd = a.pcm.size
        val midpoint = out.pcm[(overlapStart + overlapEnd) / 2]
        assertTrue(midpoint in 0.3f..0.7f, "expected the midpoint to sit between the two levels, was $midpoint")
    }

    @Test
    fun `mismatched sample rates are rejected rather than silently resampled`() {
        val a = RawAudio(FloatArray(100), sampleRate = 24_000)
        val b = RawAudio(FloatArray(100), sampleRate = 22_050)

        assertFailsWith<IllegalArgumentException> { SeamCrossfader.crossfade(a, b) }
    }

    @Test
    fun `a buffer shorter than the fade window still concatenates cleanly`() {
        val a = RawAudio(FloatArray(10) { 0.1f }, sampleRate = 24_000)
        val b = RawAudio(FloatArray(2_400) { 0.9f }, sampleRate = 24_000)

        val out = SeamCrossfader.crossfade(a, b, durationMs = 40)

        assertEquals(a.pcm.size + b.pcm.size - 10, out.pcm.size)
    }
}
