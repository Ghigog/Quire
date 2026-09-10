package quire.spike.bakeoff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import quire.spike.ParagraphUnit

/**
 * The conventions, one test each, on prose small enough to read.
 *
 * The base candidate is a double here rather than Tier 1: what is under test is the
 * turn-taking machinery, and a test that also had to get a speech tag past a real heuristic
 * would fail for two reasons at once. Tier 1's own reading is covered by [Tier1CandidateTest].
 */
class AlternationCandidateTest {

    /** A base candidate that names the speaker of whichever paragraphs it is told to. */
    private class Tags(
        private val byParagraph: Map<Int, String>,
        private val evidence: String = "speech tag",
    ) : Candidate {
        override val id = "tags"
        override val description = "test double"
        override fun answer(paragraphs: List<ParagraphUnit>, questions: List<Question>) =
            questions.associate { q ->
                val who = byParagraph[q.paragraph]
                q.id to Answer(who, if (who == null) "no tag" else evidence)
            }
    }

    /**
     * @param texts every paragraph of the passage, narration included
     * @param dialogue which of them carry a quotation, and so are turns
     * @param tags which of those the base candidate can name a speaker for
     * @return the answer for each turn, by paragraph
     */
    private fun read(
        texts: List<String>,
        tags: Map<Int, String>,
        dialogue: Set<Int> = texts.indices.toSet(),
        base: Candidate = Tags(tags),
        candidate: AlternationCandidate = AlternationCandidate(base),
    ): Map<Int, Answer> {
        val paragraphs = texts.mapIndexed { i, text -> ParagraphUnit("Test#p$i", text, 0, i) }
        val questions = dialogue.sorted().map { i ->
            Question("q$i", i, 0, texts[i].length, "Implicit", gold = "unused")
        }
        val answers = candidate.answer(paragraphs, questions)
        return questions.associate { it.paragraph to answers.getValue(it.id) }
    }

    @Test
    fun `an established pair alternates through untagged turns`() {
        val answers = read(
            texts = listOf(
                "\"Are you sure we should be here?\" said Marcus.",
                "\"It's fine. Nobody comes down this late,\" said Elena.",
                "\"I heard footsteps.\"",
                "\"That was the wind.\"",
                "\"It wasn't.\"",
            ),
            tags = mapOf(0 to "Marcus", 1 to "Elena"),
        )
        assertEquals("Marcus", answers.getValue(2).speaker)
        assertEquals("Elena", answers.getValue(3).speaker)
        assertEquals("Marcus", answers.getValue(4).speaker)
        assertEquals("alternation", answers.getValue(2).evidence)
        assertEquals("alternation x2", answers.getValue(3).evidence)
    }

    @Test
    fun `one tag is enough for the whole paragraph`() {
        // CMOS: the paragraph break is the turn boundary, so everything quoted inside one
        // paragraph is one voice — including the half that carries no tag of its own.
        val text = "\"Oh, come on,\" said John, pulling at her coat. \"We must go and see Thunderclap.\""
        val paragraphs = listOf(ParagraphUnit("Test#p0", text, 0, 0))
        val questions = listOf(
            Question("first", 0, 0, 14, "Explicit", "John"),
            Question("second", 0, 47, text.length, "Implicit", "John"),
        )
        val onlyTheFirst = object : Candidate {
            override val id = "one-tagged"
            override val description = "only the first quotation carries a tag"
            override fun answer(paragraphs: List<ParagraphUnit>, questions: List<Question>) =
                mapOf("first" to Answer("John", "speech tag"), "second" to Answer(null, "no tag"))
        }
        val answers = AlternationCandidate(onlyTheFirst).answer(paragraphs, questions)
        assertEquals("John", answers.getValue("second").speaker)
        assertEquals("same paragraph as tag", answers.getValue("second").evidence)
    }

    @Test
    fun `narration between turns re-opens the floor`() {
        // The convention is for a rapid exchange. Once the author has put a paragraph of
        // narration in, they owe the reader a fresh tag, and we decline until we get one.
        val answers = read(
            texts = listOf(
                "\"Are you sure?\" said Marcus.",
                "\"Quite sure,\" said Elena.",
                "The lamp guttered and went out. Somewhere above them a door closed.",
                "\"I heard footsteps.\"",
            ),
            tags = mapOf(0 to "Marcus", 1 to "Elena"),
            dialogue = setOf(0, 1, 3),
        )
        assertNull(answers.getValue(3).speaker)
    }

    @Test
    fun `a third speaker ends the pair until it is named again`() {
        val answers = read(
            texts = listOf(
                "\"Are you sure?\" said Marcus.",
                "\"Quite sure,\" said Elena.",
                "\"And I can hear you both,\" said the griffon.",
                "\"Run.\"",
            ),
            tags = mapOf(0 to "Marcus", 1 to "Elena", 2 to "the griffon"),
        )
        assertNull(answers.getValue(3).speaker)
    }

    @Test
    fun `an unclosed quotation carries the same speaker into the next paragraph`() {
        // The one paragraph break that is not a turn boundary: opening quote on each
        // paragraph, closing quote only at the end.
        val answers = read(CONTINUED, tags = mapOf(0 to "Marcus", 1 to "Elena"))
        assertEquals("Elena", answers.getValue(2).speaker)
        assertEquals("continued speech", answers.getValue(2).evidence)
        // And the floor did not pass while she was still speaking, so the reply is Marcus's.
        assertEquals("Marcus", answers.getValue(3).speaker)
        assertEquals("alternation", answers.getValue(3).evidence)
    }

    @Test
    fun `switching the continued-speech rule off inverts the run after it`() {
        val tags = mapOf(0 to "Marcus", 1 to "Elena")
        val blind = read(
            CONTINUED, tags,
            candidate = AlternationCandidate(Tags(tags), continuedSpeech = false),
        )
        // The second half of Elena's speech is read as Marcus taking a turn, and every
        // inferred line after it is one step out of phase. That is the cost of the rule: not
        // one line misread, but the rest of the exchange inverted.
        assertEquals("Marcus", blind.getValue(2).speaker)
        assertEquals("Elena", blind.getValue(3).speaker)
    }

    @Test
    fun `a turn nobody could attribute forgets who was speaking`() {
        // Regression: the floor used to survive a decline, so the next tag established a pair
        // with a speaker one turn stale and the rule alternated confidently on it. On PDNC that
        // showed up as the continued-speech rule scoring 11% (QUI-028 Worklog, 2026-09-10).
        val answers = read(
            texts = listOf(
                "\"Are you sure?\" said Marcus.",
                "The lamp guttered.",
                "\"No.\"",
                "\"Then we go back,\" said Elena.",
                "\"Not yet.\"",
            ),
            tags = mapOf(0 to "Marcus", 3 to "Elena"),
            dialogue = setOf(0, 2, 3, 4),
        )
        assertNull(answers.getValue(2).speaker)
        // Elena's tag is one turn of re-establishment, not two: the turn before hers is
        // unattributed, so there is no pair to alternate on yet.
        assertNull(answers.getValue(4).speaker)
    }

    /**
     * Elena's speech runs across a paragraph break: her first paragraph opens a quotation it
     * never closes, and the next opens another. Marcus is there so a pair is established.
     */
    private val CONTINUED = listOf(
        "\"Are you sure?\" said Marcus.",
        "\"You have to understand,\" said Elena. \"We were children then.",
        "\"By the time we found out, it was too late to leave.\"",
        "\"I would have left.\"",
    )

    @Test
    fun `the chain cap stops the rule running on its own guesses`() {
        val texts = listOf(
            "\"Are you sure?\" said Marcus.",
            "\"Quite sure,\" said Elena.",
            "\"I heard footsteps.\"",
            "\"That was the wind.\"",
            "\"It wasn't.\"",
        )
        val tags = mapOf(0 to "Marcus", 1 to "Elena")
        val capped = read(texts, tags, candidate = AlternationCandidate(Tags(tags), maxChain = 2))
        assertEquals("Marcus", capped.getValue(2).speaker)
        assertEquals("Elena", capped.getValue(3).speaker)
        assertNull(capped.getValue(4).speaker)
    }

    @Test
    fun `a floor seated on a weak rule is refused when strong seats are asked for`() {
        val texts = listOf(
            "Marcus set his mug down on the wooden desk. \"We don't have enough time.\"",
            "\"Then make time,\" said Elena.",
            "\"It isn't that simple.\"",
        )
        val tags = mapOf(0 to "Marcus", 1 to "Elena")
        val beats = object : Candidate {
            override val id = "beats"
            override val description = "the first turn is read from an action beat, the second from a tag"
            override fun answer(paragraphs: List<ParagraphUnit>, questions: List<Question>) =
                questions.associate { q ->
                    q.id to when (q.paragraph) {
                        0 -> Answer("Marcus", "action beat")
                        1 -> Answer("Elena", "speech tag")
                        else -> Answer(null, "no tag")
                    }
                }
        }
        assertEquals("Marcus", read(texts, tags, base = beats).getValue(2).speaker)
        val strict = read(
            texts, tags, base = beats,
            candidate = AlternationCandidate(beats, strongSeatsOnly = true),
        )
        assertNull(strict.getValue(2).speaker)
    }
}
