package quire.app.companion.pipeline

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CheckpointStoreTest {

    @Test
    fun `a written checkpoint reads back identical`() {
        val store = CheckpointStore(createTempDirectory().toFile())
        val checkpoint = ImportCheckpoint(
            bookId = "book-1",
            scanNextScene = 4,
            attributionNextScene = 2,
            entries = listOf(IndexEntrySnapshot("text", "text", emptyList(), chapter = 1)),
        )

        store.write(checkpoint)

        assertEquals(checkpoint, store.read("book-1"))
    }

    @Test
    fun `a book that was never checkpointed reads back null`() {
        val store = CheckpointStore(createTempDirectory().toFile())
        assertNull(store.read("never-imported"))
    }

    @Test
    fun `a rewrite replaces the previous checkpoint, not the temp file`() {
        val dir = createTempDirectory().toFile()
        val store = CheckpointStore(dir)

        store.write(ImportCheckpoint(bookId = "book-1", scanNextScene = 1))
        store.write(ImportCheckpoint(bookId = "book-1", scanNextScene = 2))

        assertEquals(2, store.read("book-1")?.scanNextScene)
        assertEquals(emptyList<String>(), dir.listFiles()!!.filter { it.name.endsWith(".tmp") }.map { it.name })
    }

    @Test
    fun `delete clears the checkpoint so a completed import is never mistaken for an interrupted one`() {
        val store = CheckpointStore(createTempDirectory().toFile())
        store.write(ImportCheckpoint.start("book-1"))

        store.delete("book-1")

        assertNull(store.read("book-1"))
    }
}
