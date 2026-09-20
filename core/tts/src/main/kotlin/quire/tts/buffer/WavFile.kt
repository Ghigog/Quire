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

    /**
     * The read side of the format above, for [quire.tts.engine.CloudSynthesizer] (QUI-042):
     * a cloud provider's response is bytes over HTTP, not a file this module otherwise
     * touches. Walks `fmt `/`data` chunks rather than assuming byte offset 44, because a
     * provider is free to add chunks (`LIST`, `fact`) this ticket has no reason to reject.
     * Mono 16-bit PCM only — the one shape ADR-0010 commits every provider to.
     */
    fun readPcm16Mono(bytes: ByteArray): Pair<FloatArray, Int> {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(bytes.size >= 12 && tag(buf, 0) == "RIFF" && tag(buf, 8) == "WAVE") {
            "not a RIFF/WAVE file"
        }
        var offset = 12
        var sampleRate = 0
        var channels = 1
        var bitsPerSample = 16
        var data: ByteArray? = null
        while (offset + 8 <= bytes.size) {
            val chunkId = tag(buf, offset)
            val chunkSize = buf.getInt(offset + 4)
            val body = offset + 8
            when (chunkId) {
                "fmt " -> {
                    channels = buf.getShort(body + 2).toInt()
                    sampleRate = buf.getInt(body + 4)
                    bitsPerSample = buf.getShort(body + 14).toInt()
                }
                "data" -> data = bytes.copyOfRange(body, (body + chunkSize).coerceAtMost(bytes.size))
            }
            offset = body + chunkSize + (chunkSize and 1) // chunks are word-aligned
        }
        val pcm = requireNotNull(data) { "no data chunk" }
        require(bitsPerSample == 16) { "expected 16-bit PCM, got $bitsPerSample-bit" }
        require(channels == 1) { "expected mono, got $channels channels" }

        val samples = FloatArray(pcm.size / BYTES_PER_SAMPLE)
        val pcmBuf = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)
        for (i in samples.indices) {
            samples[i] = pcmBuf.getShort(i * BYTES_PER_SAMPLE) / Short.MAX_VALUE.toFloat()
        }
        return samples to sampleRate
    }

    private fun tag(buf: ByteBuffer, at: Int) =
        String(buf.array(), at, 4, Charsets.US_ASCII)
}
