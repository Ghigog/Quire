package quire.attribution.slm

import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackgroundSlmExecutorTest {

    @Test
    fun `a normal completion runs off the calling thread and returns its result`() {
        val callingThread = Thread.currentThread()
        var ranOnThread: Thread? = null
        val runtime = SlmRuntime { prompt, _, _ ->
            ranOnThread = Thread.currentThread()
            "echo: $prompt"
        }

        val executor = BackgroundSlmExecutor(runtime)
        try {
            val handle = executor.complete("hello", maxTokens = 10)
            assertEquals("echo: hello", handle.get(1, TimeUnit.SECONDS))
        } finally {
            executor.close()
        }

        assertTrue(ranOnThread !== callingThread, "completion ran on the calling thread")
    }

    @Test
    fun `cancelling a long-running completion stops it within 500ms`() {
        val executor = BackgroundSlmExecutor(SlowCancellableSlmRuntime(tickMillis = 20))
        try {
            val handle = executor.complete("prompt", maxTokens = 10)
            Thread.sleep(50) // let a few ticks happen so cancellation lands mid-generation

            val start = System.nanoTime()
            handle.cancel()
            assertFailsWith<SlmCancelledException> { handle.get(500, TimeUnit.MILLISECONDS) }
            val elapsedMs = (System.nanoTime() - start) / 1_000_000

            assertTrue(elapsedMs < 500, "took ${elapsedMs}ms to stop after cancel")
        } finally {
            executor.close()
        }
    }
}
