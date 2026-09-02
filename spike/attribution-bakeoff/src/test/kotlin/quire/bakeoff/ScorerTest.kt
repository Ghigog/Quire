package quire.bakeoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import quire.model.Paragraph

class ScorerTest {

    private fun novel(vararg questions: Corpus.Question) = Corpus.Novel(
        meta = Corpus.NovelMeta("Test", "Test", 3, false, "literary", 1900),
        paragraphs = listOf(Paragraph("Test#p0", "text", 0, 0)),
        questions = questions.toList(),
        unscorable = 0,
        unlocatable = 0,
    )

    private fun question(id: String, gold: String, type: String = "Explicit") =
        Corpus.Question(id, 0, 0, 4, "text", type, gold)

    private class Fixed(private val answers: Map<String, Answer>) : Candidate {
        override val id = "fixed"
        override val description = "a canned answer per question"
        override fun answer(novel: Corpus.Novel) = answers
    }

    @Test
    fun `a question the candidate never answers counts against it`() {
        val score = Scorer.score(
            novel(question("a", "Sarah"), question("b", "Thomas")),
            Fixed(mapOf("a" to Answer("Sarah", "explicit tag"))),
        )
        val t = score.total
        assertEquals(2, t.scored, "both quotations are in the denominator")
        assertEquals(1, t.attributed)
        assertEquals(1, t.correct)
        assertEquals(50.0, t.coverage)
        assertEquals(100.0, t.precision)
        assertEquals(50.0, t.accuracy)
    }

    @Test
    fun `coverage and precision are reported apart`() {
        // Answering everything, half of it wrongly, beats answering a tenth perfectly on
        // accuracy and is a worse product. The numbers have to be able to say so.
        val cautious = Scorer.score(
            novel(*(1..10).map { question("q$it", "Sarah") }.toTypedArray()),
            Fixed(mapOf("q1" to Answer("Sarah"))),
        )
        val bold = Scorer.score(
            novel(*(1..10).map { question("q$it", "Sarah") }.toTypedArray()),
            Fixed((1..10).associate { "q$it" to Answer(if (it % 2 == 0) "Sarah" else "Thomas") }),
        )
        assertEquals(100.0, cautious.total.precision)
        assertEquals(10.0, cautious.total.coverage)
        assertEquals(50.0, bold.total.precision)
        assertEquals(100.0, bold.total.coverage)
    }

    @Test
    fun `tallies split by quotation type`() {
        val score = Scorer.score(
            novel(question("a", "Sarah", "Explicit"), question("b", "Sarah", "Implicit")),
            Fixed(mapOf("a" to Answer("Sarah"), "b" to Answer("Thomas"))),
        )
        assertEquals(100.0, score.byType.getValue("Explicit").accuracy)
        assertEquals(0.0, score.byType.getValue("Implicit").accuracy)
    }

    @Test
    fun `a shortened name matches its canonical form`() {
        assertTrue(Scorer.sameSpeaker("Elton", "Mr Elton"))
        assertTrue(Scorer.sameSpeaker("General", "The General"))
        assertTrue(Scorer.sameSpeaker("Elizabeth Bennet", "Elizabeth Bennet"))
    }

    @Test
    fun `honorifics still separate two people who share a surname`() {
        // Austen is a fifth of PDNC; folding titles away would score every Mrs Bennet
        // attributed to Mr Bennet as correct.
        assertFalse(Scorer.sameSpeaker("Mrs Bennet", "Mr Bennet"))
        assertFalse(Scorer.sameSpeaker("Sarah", "Thomas"))
        assertFalse(Scorer.sameSpeaker("", "Sarah"))
    }
}
