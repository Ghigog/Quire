package quire.spike

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.attribution.Roster

/** Covers the Gherkin scenarios in QUI-008. */
class Tier1Test {

    private fun attribute(text: String, roster: Set<String> = setOf("Sarah")): List<AttributionResult> {
        val unit = ParagraphUnit("t#p0", text, 0, 0)
        return Tier1.attribute(listOf(unit), roster)
    }

    private fun dialogue(text: String, roster: Set<String> = setOf("Sarah")) =
        attribute(text, roster).single { it.kind == Kind.DIALOGUE }

    // Scenario Outline: Explicit speech tags resolve
    @Test
    fun `explicit speech tags resolve with high confidence`() {
        listOf(
            "\"I know,\" said Sarah.",
            "Sarah said, \"I know.\"",
            "\"I know,\" Sarah said quietly.",
            "“I know,” said Sarah.", // typographic quotes
        ).forEach { line ->
            val r = dialogue(line)
            assertEquals("Sarah", r.speakerId, line)
            assertEquals(Tier.HEURISTIC, r.tier, line)
            assertTrue(r.confidence >= 0.9, "$line -> ${r.confidence}")
        }
    }

    // Scenario: Action beats attribute with lower confidence
    @Test
    fun `action beats attribute below explicit tag confidence`() {
        val r = dialogue("Sarah set down the cup. \"I know.\"")
        assertEquals("Sarah", r.speakerId)
        assertTrue(r.confidence < 0.9 && r.confidence >= Thresholds.SLM_MIN, "${r.confidence}")
    }

    // Scenario: Narration is not treated as dialogue
    @Test
    fun `narration is narration`() {
        val r = attribute("The rain had not let up since morning.").single()
        assertEquals(Kind.NARRATION, r.kind)
    }

    // Scenario: Unknown names do not invent characters
    @Test
    fun `a tag naming someone outside the roster attributes nobody`() {
        val r = dialogue("\"I know,\" said Gregor.", roster = setOf("Sarah"))
        assertNull(r.speakerId)
        assertEquals(Tier.NONE, r.tier)
        assertContains(r.evidence, "Gregor")
    }

    @Test
    fun `a pronoun tag is left for tier 2 with its reason recorded`() {
        val r = dialogue("\"I know,\" she said.")
        assertNull(r.speakerId)
        assertEquals("pronoun speech tag", r.evidence)
    }

    @Test
    fun `a name in addressee position is not the speaker`() {
        // Sarah is being spoken to here, so Tier 1 must not claim she is speaking.
        val r = dialogue("\"I know,\" he said to Sarah.")
        assertNull(r.speakerId)
    }

    @Test
    fun `a paragraph holding two speakers yields two dialogue segments`() {
        val segs = attribute(
            "\"I know,\" said Sarah. \"You don't,\" said Mary.",
            roster = setOf("Sarah", "Mary"),
        ).filter { it.kind == Kind.DIALOGUE }
        assertEquals(listOf("Sarah", "Mary"), segs.map { it.speakerId })
    }

    @Test
    fun `roster bootstrap separates strong and weak evidence`() {
        // Sarah speaks once with a tag, which is enough. Mary never carries a tag, so she
        // has to reach the adjacency threshold — written as the constant, not as its value,
        // because that number is tuned against PDNC and has moved once already.
        val units = listOf(ParagraphUnit("t#p0", "\"Yes,\" said Sarah.", 0, 0)) +
            (1..Roster.ADJACENCY_MIN).map { i ->
                ParagraphUnit("t#p$i", "Mary looked up. \"Really.\"", 0, i)
            }
        val roster = Tier1.bootstrapRoster(units)
        assertEquals(setOf("Sarah"), roster.fromTags.keys)
        assertEquals(Roster.ADJACENCY_MIN, roster.fromAdjacency["Mary"])
        assertContains(roster.names, "Mary")
    }

    @Test
    fun `a name seen beside a quote only once is not admitted to the roster`() {
        val units = listOf(ParagraphUnit("t#p0", "Mary put down the tray. \"No.\"", 0, 0))
        assertTrue("Mary" !in Tier1.bootstrapRoster(units).names)
    }

    // Scenario: Throughput — a 100,000 word book in under 2 seconds
    @Test
    fun `attributes a hundred thousand words in under two seconds`() {
        val sample = listOf(
            "\"I know,\" said Sarah.",
            "Thomas said, \"It is not a letter that improves with reading.\"",
            "The rain had not let up since morning, and the windows were grey with it.",
            "Sarah set down the cup and looked at him for a long moment. \"Well?\"",
            "\"You have been avoiding it,\" she said.",
        )
        val wordsPerCycle = sample.sumOf { it.split(" ").size }
        val cycles = 100_000 / wordsPerCycle
        val units = (0 until cycles).flatMap { c ->
            sample.mapIndexed { i, t -> ParagraphUnit("b#p${c * sample.size + i}", t, 0, c * sample.size + i) }
        }
        val words = units.sumOf { it.text.split(" ").size }

        val start = System.nanoTime()
        val roster = Tier1.bootstrapRoster(units)
        val results = Tier1.attribute(units, roster.names)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        println("QUI-008 throughput: $words words, ${results.size} segments, ${elapsedMs}ms")
        assertTrue(elapsedMs < 2_000, "took ${elapsedMs}ms for $words words, budget 2000ms")
    }

    @Test
    fun `fixtures attribute without a single wrong speaker`() {
        // Precision is the property Tier 1 must not lose: a confident wrong voice is
        // worse for a listener than a narrator falling back.
        val dir = File("../../fixtures/attribution")
        val files = dir.listFiles { f: File -> f.extension == "tsv" }?.sorted().orEmpty()
        assertTrue(files.isNotEmpty(), "no fixtures found at ${dir.absolutePath}")
        files.forEach { f ->
            val s = Scorer.score(Fixture.load(f))
            assertEquals(s.attributed, s.correct, "${f.name}: ${s.mistakes}")
        }
    }
}
