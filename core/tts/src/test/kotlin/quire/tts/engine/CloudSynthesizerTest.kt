package quire.tts.engine

import quire.tts.buffer.WavFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeTransport(
    private val respond: (url: String, headers: Map<String, String>, body: ByteArray) -> ByteArray,
) : HttpTransport {
    var lastUrl: String? = null
    var lastHeaders: Map<String, String>? = null
    var lastBody: String? = null

    override fun post(url: String, headers: Map<String, String>, body: ByteArray): ByteArray {
        lastUrl = url
        lastHeaders = headers
        lastBody = body.decodeToString()
        return respond(url, headers, body)
    }
}

private fun wavBytes(samples: FloatArray, sampleRate: Int): ByteArray {
    val file = kotlin.io.path.createTempFile(suffix = ".wav").toFile()
    WavFile.write(file, samples, sampleRate)
    return file.readBytes().also { file.delete() }
}

class CloudSynthesizerTest {
    private val config = CloudVoiceConfig(
        endpoint = "https://example.invalid/tts",
        apiKey = "super-secret-key",
        voiceId = "en-narrator-1",
    )

    @Test
    fun `only the line text and configured voice id are sent`() {
        val transport = FakeTransport { _, _, _ -> wavBytes(FloatArray(10), 22_050) }
        val synth = CloudSynthesizer(config, transport)

        synth.synthesize("Hello there.", voiceId = 99, cancelled = { false })

        assertEquals(config.endpoint, transport.lastUrl)
        val body = transport.lastBody.orEmpty()
        assertTrue(body.contains("\"Hello there.\""))
        assertTrue(body.contains(config.voiceId))
        assertTrue(!body.contains("99")) // the local voiceId never leaks into the cloud request
    }

    @Test
    fun `the key reaches only the Authorization header`() {
        val transport = FakeTransport { _, _, _ -> wavBytes(FloatArray(10), 22_050) }
        val synth = CloudSynthesizer(config, transport)

        synth.synthesize("hello", voiceId = 0, cancelled = { false })

        assertEquals("Bearer ${config.apiKey}", transport.lastHeaders?.get("Authorization"))
        assertTrue(!transport.lastBody.orEmpty().contains(config.apiKey))
    }

    @Test
    fun `decodes the returned wav into raw audio`() {
        val samples = FloatArray(4_410) { i -> (i % 100) / 100f - 0.5f }
        val transport = FakeTransport { _, _, _ -> wavBytes(samples, 22_050) }
        val synth = CloudSynthesizer(config, transport)

        val audio = synth.synthesize("hello", voiceId = 0, cancelled = { false })

        requireNotNull(audio)
        assertEquals(22_050, audio.sampleRate)
        assertEquals(samples.size, audio.pcm.size)
    }

    @Test
    fun `cancellation before the call skips the network entirely`() {
        var called = false
        val transport = FakeTransport { _, _, _ -> called = true; wavBytes(FloatArray(1), 22_050) }
        val synth = CloudSynthesizer(config, transport)

        val audio = synth.synthesize("hello", voiceId = 0, cancelled = { true })

        assertNull(audio)
        assertTrue(!called)
    }

    @Test
    fun `a transport failure is wrapped, never swallowed`() {
        val transport = FakeTransport { _, _, _ -> error("network down") }
        val synth = CloudSynthesizer(config, transport)

        assertFailsWith<CloudSynthesisException> {
            synth.synthesize("hello", voiceId = 0, cancelled = { false })
        }
    }

    @Test
    fun `an unreadable response is wrapped, never crashes the caller`() {
        val transport = FakeTransport { _, _, _ -> "not a wav file".encodeToByteArray() }
        val synth = CloudSynthesizer(config, transport)

        assertFailsWith<CloudSynthesisException> {
            synth.synthesize("hello", voiceId = 0, cancelled = { false })
        }
    }
}

class WavRoundTripTest {
    @Test
    fun `read is the inverse of write for mono pcm16`() {
        val samples = FloatArray(1_000) { i -> kotlin.math.sin(i * 0.1).toFloat() }
        val bytes = wavBytes(samples, 24_000)

        val (decoded, sampleRate) = WavFile.readPcm16Mono(bytes)

        assertEquals(24_000, sampleRate)
        assertEquals(samples.size, decoded.size)
        // 16-bit round trip loses precision, not samples — every value stays within one
        // quantisation step of the original.
        for (i in samples.indices) {
            assertTrue(kotlin.math.abs(samples[i] - decoded[i]) < 0.001f)
        }
    }
}
