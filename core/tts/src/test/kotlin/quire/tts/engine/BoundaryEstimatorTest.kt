package quire.tts.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoundaryEstimatorTest {

    private val text = "Hello there, friend. How are you today? I am well!"

    @Test
    fun `covers every word in ascending order`() {
        val words = BoundaryEstimator.estimate(text, 10_000).filter { it.kind == BoundaryKind.WORD }

        assertEquals(10, words.size)
        assertEquals(0L, words.first().startMs)
        assertEquals(10_000L, words.last().endMs)
        for (i in 1 until words.size) {
            assertTrue(words[i].startMs >= words[i - 1].endMs, "word $i starts before word ${i - 1} ends")
            assertTrue(words[i].charStart >= words[i - 1].charEnd, "word $i overlaps word ${i - 1} in text")
        }
    }

    @Test
    fun `a word boundary maps back to the same word in the source text`() {
        val words = BoundaryEstimator.estimate(text, 10_000).filter { it.kind == BoundaryKind.WORD }

        val fifth = words[4]
        assertEquals("are", text.substring(fifth.charStart, fifth.charEnd))
    }

    @Test
    fun `sentences are split on terminal punctuation`() {
        val sentences = BoundaryEstimator.estimate(text, 9_000).filter { it.kind == BoundaryKind.SENTENCE }

        assertEquals(3, sentences.size)
        assertEquals("Hello there, friend.", text.substring(sentences[0].charStart, sentences[0].charEnd))
        assertEquals("How are you today?", text.substring(sentences[1].charStart, sentences[1].charEnd))
        assertEquals("I am well!", text.substring(sentences[2].charStart, sentences[2].charEnd))
        assertEquals(0L, sentences.first().startMs)
        assertEquals(9_000L, sentences.last().endMs)
    }

    @Test
    fun `a single sentence with no terminal punctuation is still covered`() {
        val boundaries = BoundaryEstimator.estimate("no punctuation here", 1_000)

        val sentence = boundaries.single { it.kind == BoundaryKind.SENTENCE }
        assertEquals(0, sentence.charStart)
        assertEquals("no punctuation here".length, sentence.charEnd)
        assertEquals(3, boundaries.count { it.kind == BoundaryKind.WORD })
    }

    @Test
    fun `surrounding whitespace is trimmed from sentence spans`() {
        val boundaries = BoundaryEstimator.estimate("  padded.  ", 500)

        val sentence = boundaries.single { it.kind == BoundaryKind.SENTENCE }
        assertEquals("padded.", "  padded.  ".substring(sentence.charStart, sentence.charEnd))
    }

    @Test
    fun `zero duration produces no boundaries`() {
        assertEquals(emptyList(), BoundaryEstimator.estimate(text, 0))
    }

    @Test
    fun `empty text produces no boundaries`() {
        assertEquals(emptyList(), BoundaryEstimator.estimate("   ", 1_000))
    }
}
