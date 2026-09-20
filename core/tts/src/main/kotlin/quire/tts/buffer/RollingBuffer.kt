package quire.tts.buffer

import quire.tts.engine.Boundary
import java.io.File
import java.util.concurrent.Executors

/** Where a buffered paragraph's audio is on the way to being playable. */
sealed interface SlotState {
    /** Synthesis for this index has been requested but has not finished yet. */
    data object Pending : SlotState

    /** [file] holds the paragraph's audio, cached as a 16-bit PCM `.wav` (see [WavFile]). */
    data class Ready(val file: File, val durationMs: Long, val boundaries: List<Boundary>) : SlotState

    /** [ParagraphSynthesizer] returned `null` for a reason other than cancellation. */
    data object Failed : SlotState

    /** [ParagraphSource] has no paragraph at this index — the book ends here. */
    data object EndOfBook : SlotState
}

/**
 * Keeps the paragraph now playing plus up to three ahead synthesised and cached as `.wav`
 * files (PRD §3.2), refilling in the background as playback advances and re-seeding on a
 * seek. QUI-012.
 *
 * One background worker, one synthesis in flight at a time: [scheduleNextLocked] only ever
 * dispatches the lowest-index missing slot in the window, ascending from [currentIndex], so
 * the paragraph about to play is always synthesised before the ones ahead of it — the same
 * "one ONNX session, serialised inference" rule QUI-024 states for the same reason
 * (concurrency buys nothing at RTF 0.15 and doubles peak memory).
 *
 * Every mutation that changes what the window should contain — [seed], [seek], shrinking
 * [setDepthAhead] — is checked for at the point a background synthesis *completes*, by
 * testing whether its index still falls in the current window: an in-flight synthesis for a
 * paragraph a seek moved away from is simply not stored when it finishes, rather than
 * interrupted while running. [advance] does not invalidate anything, because the paragraph
 * a pending job targets is still wanted after the window merely slides forward under it.
 */
class RollingBuffer(
    private val source: ParagraphSource,
    private val synthesizer: ParagraphSynthesizer,
    private val cacheDir: File,
    maxDepthAhead: Int = MAX_DEPTH_AHEAD,
) : AutoCloseable {

    companion object {
        const val MIN_DEPTH_AHEAD = 1
        const val MAX_DEPTH_AHEAD = 3
    }

    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "rolling-buffer").apply { isDaemon = true }
    }
    private val lock = Any()
    private val slots = HashMap<Int, SlotState>()
    private var currentIndex = 0
    private var depthAhead = maxDepthAhead.coerceIn(MIN_DEPTH_AHEAD, MAX_DEPTH_AHEAD)

    /** Bumped only by [seed] and [seek] — a real discontinuity, not a one-paragraph slide. */
    private var epoch = 0L
    private var closed = false

    init {
        // A crash can leave a slot's .wav written but never consumed; nothing here can tell
        // a genuine leftover from one still in progress, so the only safe rule is "trust
        // none of it" (Requirements: "clean the cache directory on startup").
        cacheDir.mkdirs()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /** Starts the buffer at [startIndex] — normally 0, or a saved reading position. */
    fun seed(startIndex: Int) = synchronized(lock) { reseedLocked(startIndex) }

    /** Playback finished the paragraph at [currentIndex] and moved to the next one. */
    fun advance() = synchronized(lock) {
        deleteSlotFileLocked(currentIndex)
        slots.remove(currentIndex)
        currentIndex += 1
        scheduleNextLocked()
    }

    /** Cancels in-flight synthesis for the old window and re-seeds the buffer at [toIndex]. */
    fun seek(toIndex: Int) = synchronized(lock) { reseedLocked(toIndex) }

    /**
     * Adjusts how many paragraphs ahead of [currentIndex] to keep ready, clamped to
     * [MIN_DEPTH_AHEAD]..[MAX_DEPTH_AHEAD]. Requirements: "adaptive downward under memory
     * pressure but never below current + 1".
     */
    fun setDepthAhead(depth: Int) = synchronized(lock) {
        depthAhead = depth.coerceIn(MIN_DEPTH_AHEAD, MAX_DEPTH_AHEAD)
        val windowEnd = currentIndex + depthAhead
        slots.keys.filter { it > windowEnd }.forEach { index ->
            deleteSlotFileLocked(index)
            slots.remove(index)
        }
        scheduleNextLocked()
    }

    fun state(index: Int): SlotState? = synchronized(lock) { slots[index] }

    /** Indices in the current window whose audio is ready to play, ascending. */
    fun readyIndices(): List<Int> = synchronized(lock) {
        slots.filterValues { it is SlotState.Ready }.keys.sorted()
    }

    /** The paragraph indices this buffer is currently trying to keep filled. */
    fun window(): IntRange = synchronized(lock) { windowLocked() }

    /**
     * Blocks until [index] leaves [SlotState.Pending] or [timeoutMs] elapses (`null`).
     * Exists for tests and for measuring time-to-first-sound; production playback should
     * prefer reacting to state changes over polling.
     */
    fun awaitReady(index: Int, timeoutMs: Long): SlotState? {
        val deadlineNanos = System.nanoTime() + timeoutMs * 1_000_000
        while (true) {
            val current = synchronized(lock) { slots[index] }
            if (current != null && current !is SlotState.Pending) return current
            if (System.nanoTime() >= deadlineNanos) return null
            Thread.sleep(2)
        }
    }

    /** Cancels in-flight synthesis and deletes every cached file (session end). */
    override fun close() {
        synchronized(lock) {
            closed = true
            epoch += 1
            slots.keys.toList().forEach { deleteSlotFileLocked(it) }
            slots.clear()
        }
        worker.shutdownNow()
    }

    private fun reseedLocked(startIndex: Int) {
        epoch += 1
        slots.keys.toList().forEach { deleteSlotFileLocked(it) }
        slots.clear()
        currentIndex = startIndex
        scheduleNextLocked()
    }

    private fun windowLocked(): IntRange = currentIndex..(currentIndex + depthAhead)

    private fun deleteSlotFileLocked(index: Int) {
        (slots[index] as? SlotState.Ready)?.file?.delete()
    }

    private fun cacheFile(index: Int): File = File(cacheDir, "paragraph-$index.wav")

    private fun scheduleNextLocked() {
        if (closed) return
        val nextMissing = windowLocked().firstOrNull { it !in slots } ?: return
        slots[nextMissing] = SlotState.Pending
        val requestEpoch = epoch
        worker.execute { synthesizeSlot(nextMissing, requestEpoch) }
    }

    private fun synthesizeSlot(index: Int, requestEpoch: Long) {
        val paragraph = source.at(index)
        val chunk = paragraph?.let {
            synthesizer.synthesize(it) {
                synchronized(lock) { epoch != requestEpoch || index !in windowLocked() }
            }
        }
        synchronized(lock) {
            if (closed || epoch != requestEpoch || index !in windowLocked()) {
                // Superseded by a seek, or evicted by a shrinking depth — drop the result,
                // and drop the Pending marker so a later window can ask for it again.
                slots.remove(index)
                return@synchronized
            }
            slots[index] = when {
                paragraph == null -> SlotState.EndOfBook
                chunk == null -> SlotState.Failed
                else -> {
                    val file = cacheFile(index)
                    WavFile.write(file, chunk.pcm, chunk.sampleRate)
                    val durationMs = chunk.pcm.size.toLong() * 1000L / chunk.sampleRate
                    SlotState.Ready(file, durationMs, chunk.boundaries)
                }
            }
            scheduleNextLocked()
        }
    }
}
