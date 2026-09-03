package quire.spike.bakeoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import quire.spike.Pdnc

class BakeoffTest {

    private fun question(id: String, gold: String, type: String = "Explicit") =
        Question(id, paragraph = 0, start = 0, end = 4, type = type, gold = gold, text = "text")

    @Test
    fun `a question the candidate never answers counts against it`() {
        val score = Bakeoff.tally(
            listOf(question("a", "Sarah"), question("b", "Thomas")),
            mapOf("a" to Answer("Sarah", "speech tag")),
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
        val questions = (1..10).map { question("q$it", "Sarah") }
        val cautious = Bakeoff.tally(questions, mapOf("q1" to Answer("Sarah")))
        val bold = Bakeoff.tally(
            questions,
            (1..10).associate { "q$it" to Answer(if (it % 2 == 0) "Sarah" else "Thomas") },
        )
        assertEquals(100.0, cautious.total.precision)
        assertEquals(10.0, cautious.total.coverage)
        assertEquals(50.0, bold.total.precision)
        assertEquals(100.0, bold.total.coverage)
    }

    @Test
    fun `tallies split by quotation type`() {
        val score = Bakeoff.tally(
            listOf(question("a", "Sarah", "Explicit"), question("b", "Sarah", "Implicit")),
            mapOf("a" to Answer("Sarah"), "b" to Answer("Thomas")),
        )
        assertEquals(100.0, score.byType.getValue("Explicit").accuracy)
        assertEquals(0.0, score.byType.getValue("Implicit").accuracy)
    }

    @Test
    fun `a shortened name matches its canonical form`() {
        assertTrue(Pdnc.matches("Elton", "Mr Elton"))
        assertTrue(Pdnc.matches("General", "The General"))
        assertTrue(Pdnc.matches("Elizabeth Bennet", "Elizabeth Bennet"))
    }

    @Test
    fun `honorifics still separate two people who share a surname`() {
        // Austen is a fifth of PDNC; folding titles away would score every Mrs Bennet
        // attributed to Mr Bennet as correct.
        assertFalse(Pdnc.matches("Mrs Bennet", "Mr Bennet"))
        assertFalse(Pdnc.matches("Sarah", "Thomas"))
        assertFalse(Pdnc.matches("", "Sarah"))
    }
}
