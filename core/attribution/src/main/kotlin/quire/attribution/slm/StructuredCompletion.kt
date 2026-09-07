package quire.attribution.slm

/** The outcome of a [StructuredCompletion.complete] call. Never raw text — see the KDoc there. */
sealed interface StructuredResult<out T> {
    data class Success<T>(val value: T) : StructuredResult<T>
    data class Failure(val reason: String) : StructuredResult<Nothing>
}

/**
 * QUI-006's structured-output contract: a completion constrained to a caller-supplied
 * [JsonShape]. A malformed generation is retried exactly once, with the failure shown back
 * to the model; a second malformed generation is reported as [StructuredResult.Failure].
 * Callers never see raw text they have to parse defensively.
 */
class StructuredCompletion(private val runtime: SlmRuntime) {

    fun <T> complete(
        prompt: String,
        shape: JsonShape<T>,
        maxTokens: Int,
        cancellation: CancellationSignal = CancellationSignal(),
    ): StructuredResult<T> {
        var attemptPrompt = prompt
        var lastReason = ""

        repeat(MAX_ATTEMPTS) { attempt ->
            val raw = runtime.complete(attemptPrompt, maxTokens, cancellation)
            try {
                return StructuredResult.Success(shape.parse(raw))
            } catch (mismatch: ShapeMismatch) {
                lastReason = mismatch.message ?: "malformed structured output"
                if (attempt == 0) attemptPrompt = retryPrompt(prompt, lastReason)
            }
        }

        return StructuredResult.Failure(lastReason)
    }

    private fun retryPrompt(original: String, reason: String) = buildString {
        append(original)
        append("\n\nYour previous reply did not match the required shape (")
        append(reason)
        append("). Reply again with only the requested structure, nothing else.")
    }

    companion object {
        /** The one retry QUI-006 specifies — not a general backoff policy. */
        private const val MAX_ATTEMPTS = 2
    }
}
