package quire.app.ttsservice.synthesis

/** Float samples in `[-1, 1]` to little-endian 16-bit PCM bytes — what `audioAvailable` wants. */
object Pcm16 {

    fun encode(pcm: FloatArray): ByteArray {
        val out = ByteArray(pcm.size * 2)
        for (i in pcm.indices) {
            val v = (pcm[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
            out[i * 2] = (v and 0xFF).toByte()
            out[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return out
    }
}
