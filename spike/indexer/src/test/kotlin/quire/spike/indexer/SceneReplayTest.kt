package quire.spike.indexer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.index.How
import quire.index.InMemoryBookIndex
import quire.index.Matcher

/**
 * The vertical slice's central claim, checked without a device: given the clauses a host
 * actually sends, the index says who is speaking.
 *
 * The pair of fixtures is the point. `neoreader-epub-shape.tsv` is a real capture of how
 * NeoReader chunks EPUB text — clause-level, split at commas, continuations carrying a
 * leading space. `.labels.tsv` is the same scene as whole sentences with gold speakers.
 * Build an index from the second, replay the first against it, and the voices must be
 * right on chunks that contain no evidence of their own.
 */
class SceneReplayTest {

    private val fixtures = File("../../fixtures/host-traces")

    private fun chunks(): List<String> =
        File(fixtures, "neoreader-epub-shape.tsv").readLines().drop(1)
            .filter { it.isNotBlank() }
            .map { it.substringAfterLast('\t') }
            .filter { it.isNotBlank() }

    private fun matcher(): Matcher {
        val rows = Labelled.load(File(fixtures, "neoreader-epub-shape.labels.tsv"))
        return Matcher(InMemoryBookIndex("scene", Labelled.entries(rows)))
    }

    @Test
    fun `every chunk but the page number places in the book`() {
        val matcher = matcher()
        val unmatched = chunks().filter { !matcher.match(it).matched }
        // "1" is a page number the host emitted between the heading and the prose. It is
        // not in the book, so falling through to the narrator is the correct answer.
        assertEquals(listOf("1 "), unmatched)
    }

    @Test
    fun `a continuation clause carrying no quote mark is still the speaker`() {
        val matcher = matcher()
        val voices = chunks().associateWith { chunk ->
            matcher.match(chunk).spans.mapNotNull { it.speakerId }.distinct()
        }

        // The bug this architecture exists to avoid: these two clauses contain no opening
        // quote, so anything inferring speech from the text in front of it reads them as
        // narration. Observed on device before the index was wired in — ADR-0002 §6.
        assertTrue("Sarah" in voices.getValue(" avoiding the letter,"))
        assertTrue("Sarah" in voices.getValue(" she said."))

        // And narration on its own line stays narration, which is the mirror failure.
        assertTrue(voices.getValue("Thomas did not turn from the desk.").isEmpty())
        assertTrue(voices.getValue("He responded at last.").isEmpty())
    }

    @Test
    fun `reading forward uses the cursor rather than relocating every chunk`() {
        val matcher = matcher()
        val hows = chunks().map { matcher.match(it).how }
        // One RELOCATED to find the opening heading, then the cursor carries the rest.
        assertEquals(1, hows.count { it == How.RELOCATED })
        assertTrue(hows.count { it == How.FORWARD } >= 13)
    }
}
