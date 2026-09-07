package quire.attribution.slm

/** Returns [responses] in order, repeating the last one if asked for more than it has. */
class FakeSlmRuntime(private val responses: List<String>) : SlmRuntime {
    var callCount = 0
        private set

    override fun complete(prompt: String, maxTokens: Int, cancellation: CancellationSignal): String {
        val response = responses[callCount.coerceAtMost(responses.size - 1)]
        callCount++
        return response
    }
}

/**
 * Simulates a generation loop that only stops when [CancellationSignal] fires — the
 * cooperative-cancellation contract [SlmRuntime.complete] documents. Ticks rather than
 * sleeping once so a test can cancel mid-generation instead of racing a single call.
 */
class SlowCancellableSlmRuntime(private val tickMillis: Long = 20, private val maxTicks: Int = 1_000) : SlmRuntime {
    override fun complete(prompt: String, maxTokens: Int, cancellation: CancellationSignal): String {
        repeat(maxTicks) {
            if (cancellation.isCancelled) throw SlmCancelledException()
            Thread.sleep(tickMillis)
        }
        return "done"
    }
}
