package quire.app.ttsservice.synthesis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Pcm16Test {

    private fun sampleAt(bytes: ByteArray, index: Int): Short =
        ((bytes[index * 2 + 1].toInt() shl 8) or (bytes[index * 2].toInt() and 0xFF)).toShort()

    @Test
    fun `encodes silence as zero bytes`() {
        val out = Pcm16.encode(floatArrayOf(0f, 0f))

        assertEquals(4, out.size)
        assertTrue(out.all { it == 0.toByte() })
    }

    @Test
    fun `round-trips full-scale samples`() {
        val out = Pcm16.encode(floatArrayOf(1f, -1f))

        assertEquals(Short.MAX_VALUE, sampleAt(out, 0))
        assertEquals((-Short.MAX_VALUE).toShort(), sampleAt(out, 1))
    }

    @Test
    fun `clips out-of-range samples`() {
        val out = Pcm16.encode(floatArrayOf(2f, -2f))

        assertEquals(Short.MAX_VALUE, sampleAt(out, 0))
        assertEquals((-Short.MAX_VALUE).toShort(), sampleAt(out, 1))
    }
}
