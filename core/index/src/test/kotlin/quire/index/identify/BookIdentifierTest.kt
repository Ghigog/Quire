package quire.index.identify

import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.index.BookIndex
import quire.index.BookRecord
import quire.index.InMemoryBookIndex
import quire.index.Normalizer
import quire.index.Schema
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

class BookIdentifierTest {

    private fun book(id: String, indexedAt: Long, vararg lines: String):
        Pair<BookRecord, BookIndex> {
        val entries = lines.mapIndexed { seq, text ->
            IndexEntry(
                seq, text, Normalizer.normalize(text),
                listOf(VoiceSpan(0, text.length, Kind.NARRATION, null, 1.0)),
            )
        }
        val record = BookRecord(
            bookId = id, title = id, author = "t", entryCount = entries.size,
            indexedAt = indexedAt, schemaVersion = Schema.VERSION, sourceDigest = id,
        )
        return record to InMemoryBookIndex(id, entries)
    }

    private val moby = book("moby", 1_000, *MOBY_LINES)

    private val emma = book("emma", 2_000,
        "Emma Woodhouse, handsome, clever, and rich, seemed to unite the best blessings.",
        "She was the youngest of the two daughters of a most affectionate father.",
        "Her mother had died too long ago for her to have more than a memory.",
        "Sixteen years had Miss Taylor been in Mr Woodhouse's family.")

    private val shelf = listOf(moby, emma)

    @Test
    fun `locks on to the right book without user input`() {
        val id = BookIdentifier.over(shelf)
        listOf(
            "Call me Ishmael.",
            "Some years ago,",
            " never mind how long precisely,",
        ).forEach { chunk ->
            assertNull(id.bookId, "locked too early, on '$chunk'")
            id.accept(chunk)
        }

        // Three agreeing chunks are in; the fourth is the first one actually voiced.
        assertEquals("moby", id.bookId)
        assertTrue(id.accept(" I thought I would sail about.").matched)
    }

    @Test
    fun `reads the narrator until it is confident`() {
        val id = BookIdentifier.over(shelf)
        // An unmatched result is what makes the first chunk safe: the caller's behaviour
        // for "no match" is already the narrator, so there is one path, not two.
        assertTrue(!id.accept("Call me Ishmael.").matched)
        assertNull(id.bookId)
    }

    @Test
    fun `an unindexed book never locks on`() {
        val id = BookIdentifier.over(shelf)
        listOf(
            "It was a bright cold day in April,",
            " and the clocks were striking thirteen.",
            "Winston Smith slipped quickly through the glass doors.",
            "The hallway smelt of boiled cabbage and old rag mats.",
            "At one end of it a coloured poster had been tacked to the wall.",
        ).forEach { assertTrue(!id.accept(it).matched) }
        assertNull(id.bookId)
    }

    @Test
    fun `a nine chunk unmatched passage does not lose the lock`() {
        val id = BookIdentifier.over(shelf)
        MOBY_LINES.take(2).forEach(id::accept)
        id.accept("It is a way I have of driving off the spleen.")
        assertEquals("moby", id.bookId)

        // A quoted letter, an epigraph, a footnote the host decides to read out.
        repeat(9) { id.accept("A line of a letter that is not in the index at all.") }
        assertEquals("moby", id.bookId, "gave up the lock inside the budget")

        id.accept("A tenth line, still not in the index.")
        assertNull(id.bookId, "held the lock past the budget")
    }

    @Test
    fun `the override skips fingerprinting entirely`() {
        val id = BookIdentifier.forBook("emma", shelf)
        assertEquals("emma", id.bookId)
        // No warm-up: the very first chunk is voiced from the chosen book.
        assertTrue(id.accept("Emma Woodhouse,").matched)
    }

    @Test
    fun `two editions agreeing resolve to the newest import, and say so`() {
        val id = BookIdentifier.over(listOf(
            book("moby-1851", 1_000, *MOBY_LINES),
            book("moby-1930", 5_000, *MOBY_LINES),
        ))
        MOBY_LINES.take(3).forEach(id::accept)

        assertEquals("moby-1930", id.bookId)
        assertTrue(id.ambiguous, "locked on silently while two editions agreed")
    }

    @Test
    fun `identifying across fifty books stays well inside the per-chunk budget`() {
        // A host number, not an SLA: this is a desktop x86 core and PRD budgets are only
        // ever true on the reference device (CLAUDE.md §1.6). It is here to catch an
        // algorithmic regression — a linear scan going quadratic — not to claim a pass.
        val many = (0 until 50).map { n ->
            book("book-$n", n.toLong(), *Array(200) { "Sentence $it of book number $n." })
        }
        val chunk = "Sentence 7 of book number 23."

        // A fresh identifier each time, deliberately. Once a book is locked, accept()
        // runs one matcher rather than fifty, so timing a locked identifier would measure
        // the cheap path and report a number that means nothing. What has to fit the
        // budget is the scan across every candidate.
        repeat(20) { BookIdentifier.over(many).accept(chunk) }        // warm the JIT

        val perChunkMs =
            measureNanoTime { repeat(100) { BookIdentifier.over(many).accept(chunk) } } / 100 / 1e6
        println("identification across 50 books: %.2f ms per chunk (host)".format(perChunkMs))
        assertTrue(perChunkMs < 50.0, "took %.1f ms per chunk".format(perChunkMs))
    }

    private companion object {
        val MOBY_LINES = arrayOf(
            "Call me Ishmael.",
            "Some years ago, never mind how long precisely, I thought I would sail about.",
            "It is a way I have of driving off the spleen.",
            "Whenever I find myself growing grim about the mouth.",
        )
    }
}
