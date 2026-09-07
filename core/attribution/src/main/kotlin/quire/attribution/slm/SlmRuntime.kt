package quire.attribution.slm

/**
 * A quantized on-device language model that turns a prompt into text. QUI-006's interface
 * seam: scanning (QUI-007) and Tier 2/3 attribution (QUI-009) prompt through this without
 * knowing which backend — `llama.cpp` via JNI or ExecuTorch — is behind it, or which model
 * is loaded. ADR-0001 records the backend choice; it is not made here.
 *
 * One [SlmRuntime] holds one resident model. Loading and lifetime (lazy load, release under
 * memory pressure, never resident beside an idle TTS session past the combined RAM budget)
 * are the caller's concern — an Android service in the app layer, once one exists — because
 * this module is pure Kotlin/JVM and knows nothing about Android (CLAUDE.md §9).
 */
fun interface SlmRuntime {

    /**
     * Completes [prompt], generating at most [maxTokens] tokens of raw text.
     *
     * Implementations must run generation off the caller's thread and check [cancellation]
     * frequently enough — between generated tokens, not only at the start — that a
     * cancelled call stops within 500 ms and releases its working memory before returning
     * (QUI-006 acceptance criteria). [BackgroundSlmExecutor] gives one caller-side way to
     * get "off the main thread" for free; a JNI backend still has to poll [cancellation]
     * itself inside its own generation loop, since no JVM thread interrupt reaches native
     * code uninvited.
     *
     * @throws SlmCancelledException if [cancellation] fires before generation finishes.
     */
    fun complete(prompt: String, maxTokens: Int, cancellation: CancellationSignal): String
}
