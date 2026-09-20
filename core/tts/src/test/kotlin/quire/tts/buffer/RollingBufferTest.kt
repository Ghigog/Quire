package quire.tts.buffer

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RollingBufferTest {

    private fun tempCacheDir(): File = Files.createTempDirectory("rolling-buffer-test").toFile()

    @Test
    fun `buffer stays ahead by current plus three paragraphs`() {
        val cacheDir = tempCacheDir()
        val buffer = RollingBuffer(FakeParagraphSource(20), FakeParagraphSynthesizer(), cacheDir)
        try {
            buffer.seed(5)
            assertNotNull(buffer.awaitReady(8, timeoutMs = 2_000), "index 8 never became ready")

            assertEquals(5..8, buffer.window())
            assertEquals(listOf(5, 6, 7, 8), buffer.readyIndices())
        } finally {
            buffer.close()
        }
    }

    @Test
    fun `playback does not stutter across a paragraph boundary because the next one is pre-fetched`() {
        val cacheDir = tempCacheDir()
        val buffer = RollingBuffer(FakeParagraphSource(20), FakeParagraphSynthesizer(), cacheDir)
        try {
            buffer.seed(0)
            assertNotNull(buffer.awaitReady(3, timeoutMs = 2_000))

            buffer.advance()

            // The paragraph now playing was already synthesised before playback reached it —
            // no wait, so no audible gap.
            assertTrue(buffer.state(1) is SlotState.Ready)
        } finally {
            buffer.close()
        }
    }

    @Test
    fun `a played paragraph's cache file is deleted`() {
        val cacheDir = tempCacheDir()
        val buffer = RollingBuffer(FakeParagraphSource(20), FakeParagraphSynthesizer(), cacheDir)
        try {
            buffer.seed(0)
            val ready = buffer.awaitReady(0, timeoutMs = 2_000)
            val file = (ready as SlotState.Ready).file
            assertTrue(file.exists())

            buffer.advance()

            assertFalse(file.exists())
        } finally {
            buffer.close()
        }
    }

    @Test
    fun `seeking cancels in-flight synthesis and re-seeds the window at the new position`() {
        val cacheDir = tempCacheDir()
        val synthesizer = FakeParagraphSynthesizer()
        val buffer = RollingBuffer(FakeParagraphSource(200), synthesizer, cacheDir)
        try {
            buffer.seed(0)
            assertNotNull(buffer.awaitReady(3, timeoutMs = 2_000))

            // Block the worker mid-synthesis for the paragraph it would fetch next, so the
            // seek below has something genuinely in flight to cancel.
            synthesizer.blockIndex = 4
            buffer.advance() // window is now 1..4; index 4 starts synthesising and blocks

            buffer.seek(100)
            synthesizer.release() // let the blocked call notice cancellation and return

            assertEquals(100..103, buffer.window())
            assertNotNull(buffer.awaitReady(100, timeoutMs = 2_000), "seek did not re-seed the window")
            assertNull(buffer.state(4), "a cancelled paragraph's stale result was kept")
            assertTrue(cacheDir.listFiles()?.none { it.name == "paragraph-4.wav" } ?: true)
        } finally {
            buffer.close()
        }
    }

    @Test
    fun `depth ahead adapts downward under memory pressure but never below current plus one`() {
        val cacheDir = tempCacheDir()
        val buffer = RollingBuffer(FakeParagraphSource(20), FakeParagraphSynthesizer(), cacheDir)
        try {
            buffer.seed(0)
            assertNotNull(buffer.awaitReady(3, timeoutMs = 2_000))

            buffer.setDepthAhead(1)
            assertEquals(0..1, buffer.window())
            assertEquals(listOf(0, 1), buffer.readyIndices())

            buffer.setDepthAhead(0) // clamped
            assertEquals(RollingBuffer.MIN_DEPTH_AHEAD, buffer.window().last - buffer.window().first)
        } finally {
            buffer.close()
        }
    }

    @Test
    fun `synthesis is serialised, never more than one paragraph at a time`() {
        val cacheDir = tempCacheDir()
        val synthesizer = FakeParagraphSynthesizer()
        val buffer = RollingBuffer(FakeParagraphSource(20), synthesizer, cacheDir)
        try {
            buffer.seed(0)
            assertNotNull(buffer.awaitReady(3, timeoutMs = 2_000))

            assertEquals(1, synthesizer.maxConcurrent.get())
        } finally {
            buffer.close()
        }
    }

    @Test
    fun `a crash-orphaned cache directory is cleaned on startup`() {
        val cacheDir = tempCacheDir()
        File(cacheDir, "paragraph-0.wav").writeText("leftover from a killed process")
        File(cacheDir, "paragraph-1.wav").writeText("another leftover")

        val buffer = RollingBuffer(FakeParagraphSource(20), FakeParagraphSynthesizer(), cacheDir)
        try {
            assertEquals(0, cacheDir.listFiles()?.size ?: 0)
        } finally {
            buffer.close()
        }
    }

    @Test
    fun `closing deletes every cached file`() {
        val cacheDir = tempCacheDir()
        val buffer = RollingBuffer(FakeParagraphSource(20), FakeParagraphSynthesizer(), cacheDir)
        buffer.seed(0)
        assertNotNull(buffer.awaitReady(3, timeoutMs = 2_000))

        buffer.close()

        assertEquals(0, cacheDir.listFiles()?.size ?: 0)
    }

    @Test
    fun `an unreachable index past the end of the book is reported, not retried forever`() {
        val cacheDir = tempCacheDir()
        val buffer = RollingBuffer(FakeParagraphSource(2), FakeParagraphSynthesizer(), cacheDir)
        try {
            buffer.seed(0)

            assertEquals(SlotState.EndOfBook, buffer.awaitReady(2, timeoutMs = 2_000))
        } finally {
            buffer.close()
        }
    }
}
