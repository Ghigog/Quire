package quire.app.companion.pipeline

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import quire.index.BookRecord
import quire.index.Schema
import quire.index.SqliteBookIndex
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

class IndexPublisherTest {

    private fun entry(seq: Int, speaker: String?) = IndexEntry(
        seq = seq,
        text = "line $seq",
        normalized = "line $seq",
        spans = listOf(VoiceSpan(0, 6, Kind.DIALOGUE, speaker, 0.9)),
        chapter = 0,
    )

    private fun book(entries: List<IndexEntry>) = BookRecord(
        bookId = "book-1", title = "Title", author = "Author",
        entryCount = entries.size, indexedAt = 0L, schemaVersion = Schema.VERSION, sourceDigest = "digest",
    )

    @Test
    fun `the published database is readable through the real BookIndex`() {
        val dbFile = File(createTempDirectory().toFile(), "index.db")
        val entries = listOf(entry(0, "Sarah"), entry(1, null))

        IndexPublisher.publish(dbFile, book(entries), entries, ::JdbcSql)

        assertTrue(dbFile.exists())
        JdbcSql(dbFile.path).use { sql ->
            val index = SqliteBookIndex(sql, "book-1")
            assertEquals(2, index.size)
            assertEquals("Sarah", index.entry(0)?.spans?.single()?.speakerId)
            assertEquals(null, index.entry(1)?.spans?.single()?.speakerId)
        }
    }

    @Test
    fun `a partial write is never visible at the published path`() {
        val dbFile = File(createTempDirectory().toFile(), "index.db")
        dbFile.writeText("a previously published index")

        try {
            IndexPublisher.publish<JdbcSql>(dbFile, book(emptyList()), emptyList()) {
                throw IllegalStateException("simulated crash mid-write")
            }
        } catch (_: IllegalStateException) {
            // expected — the crash is the point of the test
        }

        assertEquals("a previously published index", dbFile.readText(), "the old index must survive a failed republish")
        assertFalse(File(dbFile.parentFile, "${dbFile.name}.tmp").exists(), "no leftover temp file either")
    }
}
