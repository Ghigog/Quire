package quire.spike.listen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The chooser decides what a person spends twenty minutes listening to, and the ways it can be
 * unfair are not visible in the audio. These are the tests for that, not for the sound.
 */
class PassagesTest {

    private fun spots(vararg paragraphs: Int) =
        paragraphs.mapIndexed { i, p -> Passages.Spot("q$i", p, 0, 10) }

    @Test
    fun `the same seed picks the same passages`() {
        val spots = spots(*(0..200 step 2).toList().toIntArray())
        val once = Passages.natural(spots, quotations = 5, maxParagraphs = 12, count = 3, seed = 7)
        val again = Passages.natural(spots, quotations = 5, maxParagraphs = 12, count = 3, seed = 7)
        assertEquals(once, again)
        assertEquals(3, once.size)
    }

    @Test
    fun `a passage never runs longer than it is allowed to`() {
        // Density is the whole reason the listen is minutes rather than half an hour: five
        // quotations spread over eighty paragraphs of narration is not a conversation.
        val sparse = spots(0, 40, 80, 120, 160)
        assertTrue(Passages.natural(sparse, 5, maxParagraphs = 12, count = 1, seed = 1).isEmpty())
        val dense = spots(0, 1, 2, 3, 4)
        assertEquals(
            listOf(Passages.Passage(0, 5)),
            Passages.natural(dense, 5, maxParagraphs = 12, count = 1, seed = 1),
        )
    }

    @Test
    fun `two passages never overlap`() {
        val spots = spots(*(0..120).toList().toIntArray())
        val picked = Passages.natural(spots, quotations = 6, maxParagraphs = 8, count = 4, seed = 3)
        for (i in picked.indices) for (j in i + 1 until picked.size) {
            val a = picked[i]
            val b = picked[j]
            assertTrue(a.toExclusive <= b.from || b.toExclusive <= a.from, "$a overlaps $b")
        }
    }

    @Test
    fun `disagreements are drawn from across the book, not from one broken chain`() {
        // The bug this replaces: a run of consecutive disagreements is mostly *one* inverted
        // alternation, so sampling inside it plays the same mistake five times and makes the
        // rule look far worse than it measures. Two runs, far apart — the sample must reach both.
        val runs = (100..115).toSet() + (900..915).toSet()
        val picked = Passages.aroundDisagreements(runs, context = 1, limit = 4, seed = 5)
        assertEquals(4, picked.size)
        assertTrue(picked.any { it.from < 200 }, "nothing from the first run: $picked")
        assertTrue(picked.any { it.from > 800 }, "nothing from the second run: $picked")
    }

    @Test
    fun `each disagreement is played with the turns around it`() {
        val picked = Passages.aroundDisagreements(setOf(50), context = 2, limit = 1, seed = 1)
        assertEquals(listOf(Passages.Passage(48, 53)), picked)
    }

    @Test
    fun `asking for more disagreements than exist yields what there is`() {
        val picked = Passages.aroundDisagreements(setOf(10, 500), context = 1, limit = 9, seed = 1)
        assertEquals(2, picked.size)
    }
}
