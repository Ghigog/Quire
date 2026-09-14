package quire.spike.bakeoff

import kotlin.test.Test
import kotlin.test.assertEquals
import quire.spike.ParagraphUnit

class CompositeCandidateTest {

    private val paragraphs = listOf(ParagraphUnit("Novel#0", "\"One.\" \"Two.\" \"Three.\"", 0, 0))

    private val questions = listOf(
        Question("q1", 0, 0, 6, "Explicit", "Alice"),
        Question("q2", 0, 7, 13, "Implicit", "Bob"),
        Question("q3", 0, 14, 22, "Anaphoric", "Carol"),
    )

    private fun fixed(name: String, answers: Map<String, Answer>) = object : Candidate {
        override val id = name
        override val description = name
        override fun answer(paragraphs: List<ParagraphUnit>, questions: List<Question>) = answers
    }

    @Test
    fun `the first candidate wins where it answered`() {
        val composite = CompositeCandidate(
            fixed("first", mapOf("q1" to Answer("Alice", "speech tag"))),
            fixed("second", mapOf("q1" to Answer("Wrong", "model"), "q2" to Answer("Bob", "model"))),
        )
        val got = composite.answer(paragraphs, questions)
        assertEquals("Alice", got["q1"]?.speaker)
        assertEquals("speech tag", got["q1"]?.evidence)
        assertEquals("Bob", got["q2"]?.speaker)
    }

    @Test
    fun `a declined answer falls through rather than blocking the fallback`() {
        // The map can carry an explicit null speaker, and Bakeoff scores that as a decline.
        // Reading a present key as an answer would leave the fallback with nothing to do,
        // which is the quiet way this class could be useless.
        val composite = CompositeCandidate(
            fixed("first", mapOf("q2" to Answer(null, "no tag"))),
            fixed("second", mapOf("q2" to Answer("Bob", "model"))),
        )
        assertEquals("Bob", composite.answer(paragraphs, questions)["q2"]?.speaker)
    }

    @Test
    fun `a quotation neither answers stays unanswered`() {
        val composite = CompositeCandidate(
            fixed("first", mapOf("q1" to Answer("Alice"))),
            fixed("second", mapOf("q2" to Answer("Bob"))),
        )
        val got = composite.answer(paragraphs, questions)
        assertEquals(setOf("q1", "q2"), got.keys)
    }

    @Test
    fun `the id names both halves in the order they run`() {
        assertEquals(
            "tier1+booknlp-plus",
            CompositeCandidate(fixed("tier1", emptyMap()), fixed("booknlp-plus", emptyMap())).id,
        )
    }
}
