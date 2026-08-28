package quire.index

import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Covers the Gherkin scenarios in QUI-021. */
class IndexStoreTest {

    private val dir: File = Files.createTempDirectory("quire-index").toFile()
    private val dbPath = File(dir, "dialogue_index.db").path

    @AfterTest fun cleanUp() { dir.deleteRecursively() }

    private fun entries(vararg lines: Pair<String, String?>) = lines.mapIndexed { seq, (text, speaker) ->
        IndexEntry(
            seq = seq,
            text = text,
            normalized = Normalizer.normalize(text),
            spans = listOf(
                VoiceSpan(
                    0, text.length,
                    if (speaker == null) Kind.NARRATION else Kind.DIALOGUE,
                    speaker, if (speaker == null) 1.0 else 0.95,
                ),
            ),
        )
    }

    private fun record(digest: String = "sha256:aaa", count: Int = 0) = BookRecord(
        bookId = "book-1", title = "A Novel", author = "Someone",
        entryCount = count, indexedAt = 1_700_000_000_000L,
        schemaVersion = Schema.VERSION, sourceDigest = digest,
    )

    private fun write(entries: List<IndexEntry>, digest: String = "sha256:aaa") {
        JdbcSql(dbPath).use { sql ->
            IndexWriter(sql).apply { createSchema() }.write(record(digest, entries.size), entries)
        }
    }

    // Scenario: Round-trip through the store
    @Test
    fun `entries round-trip in order with their speakers and confidences`() {
        val written = entries(
            "The rain had not let up since morning." to null,
            "\"I know,\" said Sarah." to "Sarah",
            "\"You don't,\" said Thomas." to "Thomas",
        )
        write(written)

        JdbcSql(dbPath, readOnly = true).use { sql ->
            val index = SqliteBookIndex(sql, "book-1")
            assertEquals(3, index.size)
            assertEquals(written.map { it.text }, (0 until index.size).map { index.entry(it)!!.text })
            assertEquals(
                listOf(null, "Sarah", "Thomas"),
                (0 until index.size).map { index.entry(it)!!.spans.single().speakerId },
            )
            assertEquals(0.95, index.entry(1)!!.spans.single().confidence)
            assertEquals(Kind.DIALOGUE, index.entry(1)!!.spans.single().kind)
            assertNull(index.entry(3), "out of range")
        }
    }

    // Scenario: Writer and matcher normalise identically
    @Test
    fun `the stored normalisation is the one the matcher computes`() {
        val text = "“I know,” said Mr. Ash­combe*."
        write(entries(text to "Mr Ashcombe"))
        JdbcSql(dbPath, readOnly = true).use { sql ->
            assertEquals(
                Normalizer.normalize(text),
                SqliteBookIndex(sql, "book-1").entry(0)!!.normalized,
            )
        }
    }

    /** The whole point of the store: the matcher behaves the same over SQLite as in memory. */
    @Test
    fun `the matcher works identically against the database`() {
        val written = entries(
            "CHAPTER TWO" to null,
            "She entered very carefully, moving silently, and stopped." to null,
            "\"I know,\" said Sarah." to "Sarah",
        )
        write(written)
        JdbcSql(dbPath, readOnly = true).use { sql ->
            val onDisk = Matcher(SqliteBookIndex(sql, "book-1"))
            val inMemory = Matcher(InMemoryBookIndex("book-1", written))
            val chunks = listOf(
                "CHAPTER TWO ", "She entered very carefully,", " moving silently,",
                " and stopped.", "\"I know,\"", " said Sarah.",
            )
            for (chunk in chunks) {
                val a = onDisk.match(chunk)
                val b = inMemory.match(chunk)
                assertEquals(b.how, a.how, chunk)
                assertEquals(b.entries.map { it.seq }, a.entries.map { it.seq }, chunk)
                assertEquals(b.spans.map { it.speakerId }, a.spans.map { it.speakerId }, chunk)
            }
            assertEquals(inMemory.cursor, onDisk.cursor)
            assertEquals(inMemory.offset, onDisk.offset)
        }
    }

    // Scenario: The service cannot corrupt the index
    @Test
    fun `a read-only connection cannot write and keeps serving`() {
        write(entries("\"I know,\" said Sarah." to "Sarah"))
        JdbcSql(dbPath, readOnly = true).use { sql ->
            val index = SqliteBookIndex(sql, "book-1")
            assertFailsWith<Exception> {
                sql.update("DELETE FROM entries WHERE book_id = ?", listOf("book-1"))
            }
            assertEquals("Sarah", index.entry(0)!!.spans.single().speakerId, "still serving after the refusal")
        }
    }

    // Scenario: A re-imported book is detected
    @Test
    fun `a different source file is visible as a different digest`() {
        write(entries("\"I know,\" said Sarah." to "Sarah"), digest = "sha256:first")
        val before = JdbcSql(dbPath, readOnly = true).use { SqliteBookIndex.books(it).single() }

        write(entries("\"I know,\" said Sarah." to "Sarah"), digest = "sha256:second")
        val after = JdbcSql(dbPath, readOnly = true).use { SqliteBookIndex.books(it).single() }

        assertNotEquals(before.sourceDigest, after.sourceDigest)
        assertEquals(1, after.entryCount, "the rewrite replaced rather than merged")
    }

    @Test
    fun `a schema version this build does not write is refused`() {
        JdbcSql(dbPath).use { sql ->
            val writer = IndexWriter(sql).apply { createSchema() }
            assertFailsWith<IllegalArgumentException> {
                writer.write(record().copy(schemaVersion = Schema.VERSION + 1), entries("Hello." to null))
            }
        }
    }

    // Scenario: Size budget
    @Test
    fun `a hundred thousand word novel indexes under five megabytes`() {
        val sample = listOf(
            "The rain had not let up since morning, and the windows were grey with it." to null,
            "\"You have been standing there for a quarter of an hour,\" said Sarah." to "Sarah",
            "\"I have been thinking,\" Thomas replied." to "Thomas",
            "He turned from the window at last, saying nothing at all." to null,
        )
        val cycles = 100_000 / sample.sumOf { it.first.split(" ").size }
        val big = entries(*(0 until cycles).flatMap { sample }.toTypedArray())
        write(big)

        val bytes = File(dbPath).length()
        val words = big.sumOf { it.text.split(" ").size }
        println("QUI-021 index size: $words words, ${big.size} entries, ${bytes / 1024} KiB")
        assertTrue(bytes < 5 * 1024 * 1024, "index is ${bytes / 1024} KiB, budget 5120 KiB")
    }
}
