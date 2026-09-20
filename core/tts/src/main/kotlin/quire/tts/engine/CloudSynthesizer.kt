package quire.tts.engine

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import quire.tts.buffer.WavFile

/**
 * The one shape a provider-shaped cloud voice is expected to accept (ADR-0010, QUI-042):
 * an endpoint, a key, a voice id, and — for now — a WAV/PCM16 response. ElevenLabs, OpenAI,
 * Sesame CSM and a reader's own endpoint all sit behind this same contract; a vendor whose
 * API differs needs a thin adapter in front of it, not a second field here.
 */
data class CloudVoiceConfig(
    val endpoint: String,
    val apiKey: String,
    val voiceId: String,
)

/** The network boundary [CloudSynthesizer] speaks through, so it is testable without sockets. */
interface HttpTransport {
    /** POSTs [body] to [url] with [headers] and returns the response body, or throws. */
    fun post(url: String, headers: Map<String, String>, body: ByteArray): ByteArray
}

/** Wraps whatever [HttpTransport] threw, so a caller never has to catch a network-library type. */
class CloudSynthesisException(message: String, cause: Throwable) : Exception(message, cause)

/**
 * A [RawSynthesizer] that hands a line to a reader-configured HTTP endpoint instead of the
 * local engine. Only [text] and the configured voice id ever leave the device — no book id,
 * no reader id, no cast (ADR-0010's boundary statement). The key lives only in the
 * `Authorization` header this class builds; it is never logged, here or anywhere upstream,
 * because this class has no logging dependency to misuse.
 */
class CloudSynthesizer(
    private val config: CloudVoiceConfig,
    private val transport: HttpTransport,
) : RawSynthesizer {

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        if (cancelled()) return null

        val body = buildJsonObject {
            put("text", JsonPrimitive(text))
            put("voice_id", JsonPrimitive(config.voiceId))
        }
        val response = try {
            transport.post(
                url = config.endpoint,
                headers = mapOf(
                    "Authorization" to "Bearer ${config.apiKey}",
                    "Content-Type" to "application/json",
                ),
                body = body.toString().encodeToByteArray(),
            )
        } catch (e: Exception) {
            throw CloudSynthesisException("cloud synthesis request failed", e)
        }
        if (cancelled()) return null

        val (samples, sampleRate) = try {
            WavFile.readPcm16Mono(response)
        } catch (e: Exception) {
            throw CloudSynthesisException("cloud synthesis returned an unreadable response", e)
        }
        return RawAudio(samples, sampleRate)
    }

    /** Nothing to release: every call is a stateless HTTP round trip. */
    override fun release() = Unit
}
