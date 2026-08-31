package quire.attribution

import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.model.Kind
import quire.model.Tier
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender

class HeuristicTest {

    private fun manifest(vararg people: Pair<String, Gender>) = CharacterManifest(
        schemaVersion = CharacterManifest.VERSION,
        bookId = "t", generatedAt = 0,
        narrator = Character("narrator", "Narrator", gender = Gender.NEUTRAL),
        characters = people.map { (name, gender) ->
            Character(id = name, displayName = name, gender = gender, confidence = 1.0)
        },
    )

    private val sarahOnly = Heuristic(manifest("Sarah" to Gender.FEMALE))

    private fun speaker(h: Heuristic, paragraph: String) =
        h.attribute("p1", paragraph).first { it.kind == Kind.DIALOGUE }

    @Test
    fun `explicit speech tags resolve, in every order`() {
        for (line in listOf(
            "\"I know,\" said Sarah.",
            "Sarah said, \"I know.\"",
            "\"I know,\" Sarah said quietly.",
        )) {
            val r = speaker(sarahOnly, line)
            assertEquals("Sarah", r.speakerId, "on: $line")
            assertEquals(Tier.HEURISTIC, r.tier)
            assertTrue(r.confidence >= 0.9, "confidence ${r.confidence} on: $line")
        }
    }

    @Test
    fun `action beats attribute with lower confidence`() {
        val r = speaker(sarahOnly, "Sarah set down the cup. \"I know.\"")
        assertEquals("Sarah", r.speakerId)
        assertTrue(r.confidence < 0.9 && r.confidence >= 0.65, "confidence was ${r.confidence}")
    }

    @Test
    fun `narration is not treated as dialogue`() {
        val results = sarahOnly.attribute("p1", "The rain had not let up since morning.")
        assertEquals(listOf(Kind.NARRATION), results.map { it.kind })
    }

    @Test
    fun `unknown names do not invent characters`() {
        val cast = manifest("Sarah" to Gender.FEMALE)
        val r = speaker(Heuristic(cast), "\"I know,\" said Gregor.")
        assertNull(r.speakerId)
        // NONE, not NARRATOR: Tier 1 declined, so QUI-009 should spend the model here.
        assertEquals(Tier.NONE, r.tier)
        // The manifest the heuristic was handed is the manifest it still has: Gregor was
        // not added as a character, which is the failure mode a looser rule would have.
        assertEquals(listOf("Sarah"), cast.characters.map { it.id })
    }

    // ---- the pronoun rule this ticket exists for ----

    @Test
    fun `a pronoun tag resolves when the cast holds one woman`() {
        val h = Heuristic(manifest("Sarah" to Gender.FEMALE, "Thomas" to Gender.MALE))
        assertEquals("Sarah", speaker(h, "\"You have been avoiding it,\" she said.").speakerId)
        assertEquals("Thomas", speaker(h, "\"It is not the letter,\" he said.").speakerId)
    }

    @Test
    fun `a pronoun tag declines when two women could be meant`() {
        // Narrowing the field is not choosing. Two candidates go to QUI-009, not to a coin.
        val h = Heuristic(manifest("Sarah" to Gender.FEMALE, "Emma" to Gender.FEMALE))
        assertNull(speaker(h, "\"You have been avoiding it,\" she said.").speakerId)
    }

    @Test
    fun `a pronoun without a speech verb is not a tag`() {
        // "She crossed to the window." is narration next to speech, not an attribution.
        val h = Heuristic(manifest("Sarah" to Gender.FEMALE))
        assertNull(speaker(h, "She crossed to the window. \"I know.\"").speakerId)
    }

    @Test
    fun `he does not match inside another word`() {
        // Whole-word matching: "the" and "then" must not read as the pronoun "he".
        val h = Heuristic(manifest("Thomas" to Gender.MALE))
        assertNull(speaker(h, "\"I know,\" the answer came.").speakerId)
    }

    @Test
    fun `em dash dialogue is segmented as speech`() {
        val h = Heuristic(manifest("Sarah" to Gender.FEMALE))
        val results = h.attribute("p1", "—I know, said Sarah.")
        assertEquals(Kind.DIALOGUE, results.first().kind)
    }

    @Test
    fun `typographic quotes work as well as straight ones`() {
        assertEquals("Sarah", speaker(sarahOnly, "“I know,” said Sarah.").speakerId)
    }

    @Test
    fun `a hundred thousand words is attributed in under two seconds`() {
        val h = Heuristic(manifest("Sarah" to Gender.FEMALE, "Thomas" to Gender.MALE))
        // ~100k words of alternating narration and tagged dialogue.
        val paragraphs = (0 until 5_000).map { n ->
            "p$n" to "The room was quiet for a while. \"I know,\" said Sarah. " +
                "Thomas did not look up from the desk, and the clock went on striking."
        }
        val words = paragraphs.sumOf { it.second.split(' ').size }
        val elapsed = measureTimeMillis { h.attributeAll(paragraphs) }
        println("Tier 1 over $words words: $elapsed ms (host)")
        assertTrue(elapsed < 2_000, "took $elapsed ms for $words words")
    }
}
