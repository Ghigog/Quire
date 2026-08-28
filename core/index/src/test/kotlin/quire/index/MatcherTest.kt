package quire.index

import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Covers the Gherkin scenarios in QUI-022. */
class MatcherTest {

    /** Build an index from sentences, each voiced wholly by one speaker. */
    private fun indexOf(vararg lines: Pair<String, String?>) = InMemoryBookIndex(
        bookId = "book",
        entries = lines.mapIndexed { seq, (text, speaker) ->
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
        },
    )

    private val chapter = indexOf(
        "The rain had not let up since morning." to null,
        "\"You have been standing there for a quarter of an hour,\" said Sarah." to "Sarah",
        "\"I have been thinking,\" Thomas replied." to "Thomas",
        "\"Well,\"" to "Sarah",
        "He turned from the window at last." to null,
        "\"Well,\"" to "Thomas",
        "The clock in the hall struck four." to null,
    )

    // Scenario: Sequential reading advances the cursor
    @Test
    fun `reading forward advances the cursor`() {
        val m = Matcher(chapter)
        m.seek(0)
        val r = m.match("\"You have been standing there for a quarter of an hour,\" said Sarah.")
        assertEquals(How.FORWARD, r.how)
        assertEquals(1, m.cursor)
        assertEquals("Sarah", r.spans.single().speakerId)
    }

    // Scenario: Identical text with different speakers resolves by position
    @Test
    fun `identical text with two speakers resolves by position`() {
        val m = Matcher(chapter)
        m.seek(2)
        assertEquals("Sarah", m.match("\"Well,\"").spans.single().speakerId)
        assertEquals(3, m.cursor)

        m.seek(4)
        assertEquals("Thomas", m.match("\"Well,\"").spans.single().speakerId)
        assertEquals(5, m.cursor)
    }

    // Scenario: A skipped heading does not lose the cursor
    @Test
    fun `a skipped entry is stepped over`() {
        val m = Matcher(chapter)
        m.seek(2)
        val r = m.match("He turned from the window at last.")
        assertEquals(How.FORWARD, r.how)
        assertEquals(4, m.cursor)
    }

    // Scenario: A jump relocates
    @Test
    fun `a jump relocates the cursor`() {
        val m = Matcher(chapter)
        m.seek(0)
        val r = m.match("The clock in the hall struck four.")
        assertEquals(How.RELOCATED, r.how)
        assertEquals(6, m.cursor)
    }

    @Test
    fun `the first chunk of a session locates without a cursor`() {
        val m = Matcher(chapter)
        val r = m.match("\"I have been thinking,\" Thomas replied.")
        assertEquals(How.RELOCATED, r.how)
        assertEquals(2, m.cursor)
    }

    // Scenario: One chunk covering several entries
    @Test
    fun `one chunk covering three entries returns all three in order`() {
        val m = Matcher(chapter)
        m.seek(0)
        val r = m.match(
            "\"You have been standing there for a quarter of an hour,\" said Sarah. " +
                "\"I have been thinking,\" Thomas replied. \"Well,\"",
        )
        assertEquals(listOf(1, 2, 3), r.entries.map { it.seq })
        assertEquals(listOf("Sarah", "Thomas", "Sarah"), r.spans.map { it.speakerId })
        assertEquals(3, m.cursor)
    }

    /**
     * The NeoReader case from ADR-0004: a heading with no terminal punctuation is glued
     * to the sentence beneath it and arrives as one chunk.
     */
    @Test
    fun `a heading glued to the following sentence still matches`() {
        val index = indexOf(
            "Chapter Two" to null,
            "The rain had not let up since morning." to null,
            "\"Well,\" said Sarah." to "Sarah",
        )
        val m = Matcher(index)
        val r = m.match("Chapter Two The rain had not let up since morning.")
        assertTrue(r.matched)
        assertEquals(listOf(0, 1), r.entries.map { it.seq })
        assertEquals(1, m.cursor)
    }

    // Scenario: A fragment matches with an offset
    @Test
    fun `a host splitting a sentence matches partially and holds the cursor`() {
        val m = Matcher(chapter)
        m.seek(0)
        val r = m.match("\"You have been standing there")
        assertTrue(r.partial)
        assertEquals(1, r.entries.single().seq)
        assertEquals(1, m.cursor, "cursor stays on the sentence still being spoken")
    }

    // Scenario: An unmatched chunk falls back at once
    @Test
    fun `an unmatched chunk yields nothing and leaves the cursor alone`() {
        val m = Matcher(chapter)
        m.seek(3)
        val r = m.match("This sentence is in no book we have indexed.")
        assertEquals(How.NONE, r.how)
        assertFalse(r.matched)
        assertTrue(r.spans.isEmpty())
        assertEquals(3, m.cursor)
    }

    @Test
    fun `a re-read after a page turn does not trigger relocation`() {
        val m = Matcher(chapter)
        m.seek(1)
        val r = m.match("\"You have been standing there for a quarter of an hour,\" said Sarah.")
        assertEquals(How.FORWARD, r.how)
        assertEquals(1, m.cursor)
    }

    @Test
    fun `spans are rebased onto the chunk the host supplied`() {
        val index = indexOf("Chapter Two" to null, "\"Well,\" said Sarah." to "Sarah")
        val m = Matcher(index)
        val chunk = "Chapter Two \"Well,\" said Sarah."
        val r = m.match(chunk)
        val second = r.spans.last()
        assertEquals("\"Well,\" said Sarah.", chunk.substring(second.start, second.end))
    }

    // Scenario: Matching is fast enough to be invisible
    @Test
    fun `matching a hundred thousand word index stays well under the budget`() {
        val sample = listOf(
            "The rain had not let up since morning." to null,
            "\"You have been standing there,\" said Sarah." to "Sarah",
            "\"I have been thinking,\" Thomas replied." to "Thomas",
            "He turned from the window at last." to null,
        )
        val cycles = 100_000 / sample.sumOf { it.first.split(" ").size }
        val repeated: List<Pair<String, String?>> = (0 until cycles).flatMap { sample }
        val big = indexOf(*repeated.toTypedArray())
        val m = Matcher(big)

        val chunks = (0 until 500).map { big.entry(it)!!.text }
        m.seek(-1)
        val start = System.nanoTime()
        var matched = 0
        for (c in chunks) if (m.match(c).matched) matched++
        val meanMicros = (System.nanoTime() - start) / 1_000 / chunks.size

        println("QUI-022 matching: ${big.size} entries, $matched/${chunks.size} matched, ${meanMicros}µs mean")
        assertEquals(chunks.size, matched)
        assertTrue(meanMicros < 10_000, "mean ${meanMicros}µs exceeds the 10ms budget")
    }
}

class NormalizerTest {

    @Test
    fun `splits on terminal punctuation and keeps abbreviations whole`() {
        assertEquals(
            listOf("Mr. Ashcombe crossed the room.", "Nobody heard him."),
            Normalizer.sentences("Mr. Ashcombe crossed the room. Nobody heard him."),
        )
    }

    @Test
    fun `a fragment with no terminal punctuation is its own sentence`() {
        assertEquals(listOf("Chapter Two"), Normalizer.sentences("Chapter Two"))
    }

    @Test
    fun `typographic and straight quotes normalise identically`() {
        assertEquals(
            Normalizer.normalize("“I know,” said Sarah."),
            Normalizer.normalize("\"I know,\" said Sarah."),
        )
    }

    @Test
    fun `soft hyphens and footnote markers disappear`() {
        assertEquals("morning", Normalizer.normalize("mor­ning*"))
    }

    @Test
    fun `apostrophes survive so contractions do not split`() {
        assertEquals("i don't know", Normalizer.normalize("I don't know."))
    }

    @Test
    fun `an em dash becomes a word boundary rather than vanishing`() {
        assertEquals("i know i said", Normalizer.normalize("I know—I said"))
    }
}
