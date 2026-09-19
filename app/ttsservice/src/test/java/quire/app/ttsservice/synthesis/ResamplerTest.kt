package quire.app.ttsservice.synthesis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import quire.tts.engine.Boundary
import quire.tts.engine.BoundaryKind

class ResamplerTest {

    @Test
    fun `factor 1 is a no-op`() {
        val pcm = floatArrayOf(0.1f, 0.2f, 0.3f)

        assertSame(pcm, Resampler.stretch(pcm, 1.0))
    }

    @Test
    fun `stretching by 2 halves the sample count`() {
        val pcm = FloatArray(1_000) { it / 1_000f }

        assertEquals(500, Resampler.stretch(pcm, 2.0).size)
    }

    @Test
    fun `slowing by half doubles the sample count`() {
        val pcm = FloatArray(500) { it / 500f }

        assertEquals(1_000, Resampler.stretch(pcm, 0.5).size)
    }

    @Test
    fun `rejects a non-positive factor`() {
        assertFailsWith<IllegalArgumentException> { Resampler.stretch(floatArrayOf(1f), 0.0) }
        assertFailsWith<IllegalArgumentException> { Resampler.stretch(floatArrayOf(1f), -1.0) }
    }

    @Test
    fun `boundary scaling divides timing by the factor, leaving text offsets untouched`() {
        val boundary = Boundary(startMs = 100, endMs = 300, charStart = 5, charEnd = 10, kind = BoundaryKind.WORD)

        val scaled = Resampler.scale(boundary, 2.0)

        assertEquals(50L, scaled.startMs)
        assertEquals(150L, scaled.endMs)
        assertEquals(5, scaled.charStart)
        assertEquals(10, scaled.charEnd)
    }

    @Test
    fun `factor 1 leaves a boundary unchanged`() {
        val boundary = Boundary(startMs = 100, endMs = 300, charStart = 5, charEnd = 10, kind = BoundaryKind.WORD)

        assertSame(boundary, Resampler.scale(boundary, 1.0))
    }
}
