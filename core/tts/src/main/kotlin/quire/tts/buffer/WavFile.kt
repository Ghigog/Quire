package quire.tts.buffer

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Writes a [quire.tts.engine.TtsChunk]'s mono float PCM to a 16-bit PCM `.wav` file — the
 * ring buffer's cache format (QUI-012's "temporary `.wav` cache files"). A buffered
 * paragraph's samples live on disk between synthesis and playback rather than resident in
 * the JVM heap for as long as it sits in the window.
 */
object WavFile {
    private const val HEADER_BYTES = 44
    private const val BYTES_PER_SAMPLE = 2

    fun write(file: File, pcm: FloatArray, sampleRate: Int) {
        file.parentFile?.mkdirs()
        val dataBytes = pcm.size * BYTES_PER_SAMPLE
        val out = ByteBuffer.allocate(HEADER_BYTES + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        writeHeader(out, dataBytes, sampleRate)
        for (sample in pcm) {
            val clamped = sample.coerceIn(-1f, 1f)
            out.putShort((clamped * Short.MAX_VALUE).roundToInt().toShort())
        }
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(out.array())
        }
    }

    private fun writeHeader(out: ByteBuffer, dataBytes: Int, sampleRate: Int) {
        val byteRate = sampleRate * BYTES_PER_SAMPLE
        out.put("RIFF".toByteArray(Charsets.US_ASCII))
        out.putInt(36 + dataBytes)
        out.put("WAVE".toByteArray(Charsets.US_ASCII))
        out.put("fmt ".toByteArray(Charsets.US_ASCII))
        out.putInt(16) // PCM fmt chunk size
        out.putShort(1) // PCM format
        out.putShort(1) // mono
        out.putInt(sampleRate)
        out.putInt(byteRate)
        out.putShort((BYTES_PER_SAMPLE).toShort()) // block align
        out.putShort((BYTES_PER_SAMPLE * 8).toShort()) // bits per sample
        out.put("data".toByteArray(Charsets.US_ASCII))
        out.putInt(dataBytes)
    }
}
