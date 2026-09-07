package quire.attribution.slm

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Runs one [SlmRuntime] off the caller's thread, per QUI-006's "all inference ... runs off
 * the main thread". A single dedicated thread, not a pool: one model held resident, one
 * completion in flight at a time — matching the runtime's own lifetime.
 *
 * [complete] returns a [Handle] immediately. [Handle.cancel] fires the [CancellationSignal]
 * the runtime call is asked to honour and returns without waiting for the worker to notice;
 * how quickly the worker actually stops is the runtime implementation's responsibility (see
 * [SlmRuntime.complete]'s KDoc).
 */
class BackgroundSlmExecutor(private val runtime: SlmRuntime) : AutoCloseable {

    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "slm-runtime").apply { isDaemon = true }
    }

    fun complete(prompt: String, maxTokens: Int): Handle {
        val cancellation = CancellationSignal()
        val future = CompletableFuture.supplyAsync(
            { runtime.complete(prompt, maxTokens, cancellation) },
            worker,
        )
        return Handle(future, cancellation)
    }

    /** Stops accepting work and interrupts whatever is running. */
    override fun close() {
        worker.shutdownNow()
    }

    class Handle internal constructor(
        private val future: CompletableFuture<String>,
        private val cancellation: CancellationSignal,
    ) {
        /** Requests cancellation; does not block. */
        fun cancel() = cancellation.cancel()

        /** Blocks for the result, or rethrows [SlmCancelledException] once cancelled. */
        fun get(timeout: Long, unit: TimeUnit): String =
            try {
                future.get(timeout, unit)
            } catch (wrapped: java.util.concurrent.ExecutionException) {
                throw wrapped.cause ?: wrapped
            }
    }
}
