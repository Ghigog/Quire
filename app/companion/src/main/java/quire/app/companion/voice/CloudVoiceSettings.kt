package quire.app.companion.voice

/**
 * A reader's own cloud voice configuration (QUI-042, ADR-0010). Off by default — [enabled]
 * is `false` until a reader fills in an endpoint and a key, and the offline tier is
 * byte-for-byte what it was before this screen existed.
 */
data class CloudVoiceSettings(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val apiKey: String = "",
    val voiceId: String = "",
    /** What the reader's own provider charges, from their rate card — Quire has no account to look this up. */
    val pricePerThousandChars: Double = 0.0,
) {
    val isConfigured: Boolean get() = endpoint.isNotBlank() && apiKey.isNotBlank() && voiceId.isNotBlank()
}
