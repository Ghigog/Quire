package quire.attribution

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.model.Kind
import quire.model.Tier
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender

class ConversationTest {

    private fun manifest(vararg people: Pair<String, Gender>) = CharacterManifest(
        schemaVersion = CharacterManifest.VERSION, bookId = "t", generatedAt = 0,
        narrator = Character("narrator", "Narrator", gender = Gender.NEUTRAL),
        characters = people.map { (n, g) -> Character(id = n, displayName = n, gender = g, confidence = 1.0) },
    )

    private fun run(manifest: CharacterManifest, vararg paragraphs: String) =
        Conversation.resolve(
            Heuristic(manifest).attributeAll(paragraphs.mapIndexed { i, t -> "p$i" to t }),
            cast = manifest.characters.map { it.id },
        )

    @Test
    fun `an untagged exchange alternates from the one tagged line`() {
        val results = run(
            manifest("Ellen" to Gender.FEMALE, "Robert" to Gender.MALE),
            "\"You will miss it,\" said Ellen.",
            "\"There is another at nine.\"",
            "\"There is not.\"",
            "\"Then I shall wait for the one at nine tomorrow.\"",
        ).filter { it.kind == Kind.DIALOGUE }

        assertEquals(listOf("Ellen", "Robert", "Ellen", "Robert"), results.map { it.speakerId })
        assertEquals(Tier.SCENE, results[1].tier)
        assertTrue(results[1].confidence < Heuristic.DIRECT_TAG)
    }

    @Test
    fun `a return to the only named speaker is recognised even when the cast is not a duet`() {
        // A larger book's cast, so `cast.size == 2` cannot bootstrap a guess at who the
        // second party is — Nivellen is tagged, his listener never is, exactly the shape
        // dylan.growcoot@ioet.com found reading Witcher 1 on device.
        val results = run(
            manifest("Nivellen" to Gender.MALE, "Geralt" to Gender.MALE, "Vereena" to Gender.FEMALE),
            "\"You want to know why,\" said Nivellen.",
            "\"I want to know everything.\"",
            "\"Then listen.\"",
        ).filter { it.kind == Kind.DIALOGUE }

        assertEquals(listOf("Nivellen", null, "Nivellen"), results.map { it.speakerId })
        assertEquals(Tier.SCENE, results.last().tier)
        assertEquals("revert to Nivellen", results.last().evidence)
    }

    @Test
    fun `the revert keeps alternating for as long as nobody else is named`() {
        val results = run(
            manifest("Nivellen" to Gender.MALE, "Geralt" to Gender.MALE, "Vereena" to Gender.FEMALE),
            "\"You want to know why,\" said Nivellen.",
            "\"I want to know everything.\"",
            "\"Then listen.\"",
            "\"I'm listening.\"",
            "\"It began years ago.\"",
        ).filter { it.kind == Kind.DIALOGUE }

        // Nivellen's listener is never once named, so this never claims to know who they
        // are — every other turn simply reverts to the one name on record.
        assertEquals(listOf("Nivellen", null, "Nivellen", null, "Nivellen"), results.map { it.speakerId })
    }

    @Test
    fun `a real second tag hands off to ordinary alternation instead`() {
        val results = run(
            manifest("Nivellen" to Gender.MALE, "Geralt" to Gender.MALE, "Vereena" to Gender.FEMALE),
            "\"You want to know why,\" said Nivellen.",
            "\"I want to know everything,\" said Geralt.",
            "\"Then listen.\"",
        ).filter { it.kind == Kind.DIALOGUE }

        // Once Geralt is actually named, this is a real established pair — the normal
        // `seen.size == 2` alternation answers it, not the solo-revert fallback.
        assertEquals(listOf("Nivellen", "Geralt", "Nivellen"), results.map { it.speakerId })
        assertEquals("alternation after Geralt", results.last().evidence)
    }

    @Test
    fun `a beat of narration does not end the exchange`() {
        val results = run(
            manifest("Ellen" to Gender.FEMALE, "Robert" to Gender.MALE),
            "\"It does not,\" said Ellen.",
            "A porter went past with a trolley, and they stood apart to let him through.",
            "\"You could come.\"",
        ).filter { it.kind == Kind.DIALOGUE }
        assertEquals("Robert", results.last().speakerId)
    }

    @Test
    fun `a scene break does end it`() {
        val results = run(
            manifest("Ellen" to Gender.FEMALE, "Robert" to Gender.MALE),
            "\"It does not,\" said Ellen.",
            "The guard's whistle went somewhere down the platform.",
            "The train pulled away and the platform emptied.",
            "Rain began against the high windows of the station roof.",
            "\"You could come.\"",
        ).filter { it.kind == Kind.DIALOGUE }
        // Whoever spoke three paragraphs ago is no guide to who speaks now.
        assertNull(results.last().speakerId)
    }

    @Test
    fun `three speakers in the room stops it guessing`() {
        val results = run(
            manifest("Ellen" to Gender.FEMALE, "Robert" to Gender.MALE, "Dana" to Gender.FEMALE),
            "\"You will miss it,\" said Ellen.",
            "\"There is another at nine,\" said Robert.",
            "\"There is not,\" said Dana.",
            "\"Then I shall wait.\"",
        ).filter { it.kind == Kind.DIALOGUE }
        assertNull(results.last().speakerId, "alternation is a coin toss with three in the room")
    }

    @Test
    fun `it never overrules an attribution tier 1 made`() {
        val results = run(
            manifest("Ellen" to Gender.FEMALE, "Robert" to Gender.MALE),
            "\"You will miss it,\" said Ellen.",
            "\"There is another at nine.\"",
            "\"And yet,\" said Ellen.",
        ).filter { it.kind == Kind.DIALOGUE }
        assertEquals("Ellen", results.last().speakerId)
        assertEquals(Tier.HEURISTIC, results.last().tier)
    }

    @Test
    fun `scored on the fixture Tier 1 cannot touch`() {
        val lines = File("../../fixtures/attribution/untagged.tsv").readLines()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { it.split('\t', limit = 2) }
        val cast = manifest("Ellen" to Gender.FEMALE, "Robert" to Gender.MALE)
        val paragraphs = lines.mapIndexed { i, (_, text) -> "p$i" to text }

        val tier1 = Heuristic(cast).attributeAll(paragraphs)
        val withTurns = Conversation.resolve(tier1, cast = cast.characters.map { it.id })

        fun score(rs: List<quire.model.AttributionResult>): Pair<Int, Int> {
            var attributed = 0
            var correct = 0
            for ((i, row) in lines.withIndex()) {
                val gold = row[0]
                if (gold == "NARRATION" || gold == "UNKNOWN") continue
                val guess = rs.filter { it.locator.startsWith("p$i#") && it.kind == Kind.DIALOGUE }
                    .firstNotNullOfOrNull { it.speakerId } ?: continue
                attributed++
                if (guess == gold) correct++
            }
            return attributed to correct
        }

        val gold = lines.count { it[0] != "NARRATION" && it[0] != "UNKNOWN" }
        val (a1, c1) = score(tier1)
        val (a2, c2) = score(withTurns)
        println("Tier 1 alone     : %d/%d attributed, %d correct (%.0f%% coverage, %.0f%% precision)"
            .format(a1, gold, c1, a1 * 100.0 / gold, c1 * 100.0 / a1))
        println("with turn-taking : %d/%d attributed, %d correct (%.0f%% coverage, %.0f%% precision)"
            .format(a2, gold, c2, a2 * 100.0 / gold, c2 * 100.0 / a2))

        assertTrue(a2 > a1, "turn-taking resolved nothing")
        assertTrue(c2.toDouble() / a2 >= 0.8, "precision fell to ${c2 * 100 / a2}%")
    }
}
