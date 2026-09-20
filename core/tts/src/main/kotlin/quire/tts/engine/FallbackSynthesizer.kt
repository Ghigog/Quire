package quire.tts.engine

/**
 * Falls back to [local] whenever [primary] fails, so a dead network mid-chapter drops the
 * voice rather than the book (QUI-042, ADR-0010: "falling back must be audible, not
 * silent"). [onFallback] fires once per [resetFallbackNotice] window — a chapter boundary,
 * in practice — because the reader needs to be told the voice changed, not told it on every
 * line for the rest of the chapter.
 */
class FallbackSynthesizer(
    private val primary: RawSynthesizer,
    private val local: RawSynthesizer,
    private val onFallback: () -> Unit = {},
) : RawSynthesizer {

    private var announced = false

    override fun synthesize(text: String, voiceId: Int, cancelled: () -> Boolean): RawAudio? {
        val fromPrimary = runCatching { primary.synthesize(text, voiceId, cancelled) }.getOrNull()
        if (fromPrimary != null) return fromPrimary
        // Null here is ambiguous — a clean cancellation and a swallowed exception both look
        // like it — so ask [cancelled] to tell them apart rather than falling back on a
        // cancelled line the reader never asked to hear from the cloud in the first place.
        if (cancelled()) return null

        if (!announced) {
            announced = true
            onFallback()
        }
        return local.synthesize(text, voiceId, cancelled)
    }

    /** Called at the next chapter boundary so a fresh network failure is announced again. */
    fun resetFallbackNotice() {
        announced = false
    }

    override fun release() {
        primary.release()
        local.release()
    }
}
