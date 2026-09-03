package quire.spike.bakeoff

import kotlin.test.Test
import kotlin.test.assertEquals
import quire.spike.ParagraphUnit

/**
 * The candidate adapter's one real job is turning a byte-located question into the right
 * segment. A paragraph holding two speakers is where that goes wrong.
 */
class Tier1CandidateTest {

    private fun ask(paragraph: String, quote: String, gold: String = "Sarah"): Answer {
        val start = paragraph.indexOf(quote)
        require(start >= 0) { "quote not in paragraph" }
        val question = Question("q", 0, start, start + quote.length, "Explicit", gold, quote)
        return Tier1Candidate()
            .answer(listOf(ParagraphUnit("Test#p0", paragraph, 0, 0)), listOf(question))
            .getValue("q")
    }

    @Test
    fun `the question picks out its own speaker in a two-voice paragraph`() {
        val paragraph = "\"I know,\" said Sarah. \"You do not,\" said Thomas."
        assertEquals("Sarah", ask(paragraph, "\"I know,\"").speaker)
        assertEquals("Thomas", ask(paragraph, "\"You do not,\"").speaker)
    }

    @Test
    fun `speech the segmenter never saw is a miss, not a gap`() {
        // No quote marks at all: on device this reads in the narrator's voice, so the
        // harness has to count it as an unanswered question rather than skip it.
        val answer = ask("Sarah looked up. I know, she thought.", "I know")
        assertEquals(null, answer.speaker)
        assertEquals("no dialogue segment", answer.evidence)
    }

    @Test
    fun `leading whitespace does not shift the answer`() {
        val paragraph = "    \"I know,\" said Sarah."
        assertEquals("Sarah", ask(paragraph, "\"I know,\"").speaker)
    }
}
