package quire.attribution.slm

import java.util.concurrent.atomic.AtomicBoolean

/**
 * A cooperative cancellation flag for one [SlmRuntime.complete] call. Deliberately not
 * `kotlinx.coroutines.Job`: this module stays dependency-free JVM (CLAUDE.md §9), and a
 * flag an implementation polls maps directly onto a native generation loop — there is no
 * JVM thread interrupt to catch inside JNI code.
 *
 * One instance is scoped to one call. [cancel] is safe to call from any thread, including
 * after the call has already finished.
 */
class CancellationSignal {
    private val cancelled = AtomicBoolean(false)

    val isCancelled: Boolean
        get() = cancelled.get()

    fun cancel() {
        cancelled.set(true)
    }
}

/** Thrown by an [SlmRuntime] when a call is stopped by its [CancellationSignal]. */
class SlmCancelledException : Exception("SLM completion was cancelled")
