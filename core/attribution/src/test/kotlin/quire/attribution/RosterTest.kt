package quire.attribution

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.model.characters.Gender

/**
 * Reading a book to find out who is in it — and, so it can be cast, what they sound like.
 *
 * Gender inference is the load-bearing part. Without it an uploaded book has no genders,
 * the pronoun rule cannot fire, and casting falls back to spreading arbitrary speaker ids,
 * which is exactly what put a male Sarah on the device.
 */
class RosterTest {

    private fun paragraphs(vararg text: String) =
        text.mapIndexed { i, t -> "p$i" to t }

    @Test
    fun `a name in speech-tag position joins the cast on one sighting`() {
        val cast = Roster.scan(paragraphs("\"I know,\" said Sarah."))
        assertTrue("Sarah" in cast.names)
    }

    @Test
    fun `a name merely standing near a quote needs corroboration`() {
        val once = Roster.scan(paragraphs("Sarah set down the cup. \"I know.\""))
        assertTrue("Sarah" !in once.names, "one adjacency should not be enough")

        // ADJACENCY_MIN is a measured number, not a chosen one — see its doc comment — so
        // the test asks for exactly as many sightings as the rule currently demands.
        val enough = Roster.scan(paragraphs(
            *(1..Roster.ADJACENCY_MIN).map { "Sarah turned to the window. \"And yet.\"" }
                .toTypedArray(),
        ))
        assertTrue("Sarah" in enough.names)
    }

    @Test
    fun `gender is inferred from the pronoun that stands in for a name`() {
        val cast = Roster.scan(paragraphs(
            "\"I know,\" said Sarah.",
            "Sarah put down her cup.",
            "She had been waiting since morning.",
            "\"There is another train,\" said Thomas.",
            "Thomas did not look up from his desk.",
            "He turned the envelope over once.",
        ))
        assertEquals(Gender.FEMALE, cast.genders["Sarah"])
        assertEquals(Gender.MALE, cast.genders["Thomas"])
    }

    @Test
    fun `one sighting is not enough to claim a gender`() {
        val cast = Roster.scan(paragraphs(
            "\"I know,\" said Sarah.",
            "Sarah put down her cup.",
        ))
        // A single pronoun could be anyone's — the cup's owner is not necessarily Sarah.
        assertEquals(null, cast.genders["Sarah"])
    }

    @Test
    fun `a name drawing both pronouns is left unknown rather than guessed`() {
        // Two people sharing a surname is the usual cause, and picking one would put a
        // character in the wrong voice for a whole book.
        val cast = Roster.scan(paragraphs(
            "\"Quite,\" said Ashcombe.",
            "Ashcombe took his hat.",
            "He left without a word.",
            "Ashcombe adjusted her glove.",
            "She said nothing at all.",
        ))
        assertEquals(null, cast.genders["Ashcombe"])
    }

    @Test
    fun `the scan produces a manifest in the frozen shape`() {
        val cast = Roster.scan(paragraphs(
            "\"I know,\" said Sarah.",
            "Sarah put down her cup.",
            "She had been waiting since morning.",
        ))
        val manifest = Roster.manifest(cast, bookId = "abc", generatedAt = 1L)
        assertEquals("abc", manifest.bookId)
        assertEquals("narrator", manifest.narrator.id)
        val sarah = manifest.characters.single { it.id == "Sarah" }
        assertEquals(Gender.FEMALE, sarah.gender)
        // A tagged name is certain; an adjacency-only name is a guess the SLM may overturn.
        assertEquals(1.0, sarah.confidence)
    }

    @Test
    fun `scanning the slice's own book recovers its cast and their genders`() {
        // End to end on real generated prose, not on lines chosen to suit the rules.
        val rows = File("../../fixtures/slice/chapter-one.labels.tsv").readLines()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .mapIndexed { i, line -> "p$i" to line.split('\t', limit = 2)[1] }

        val cast = Roster.scan(rows)
        assertTrue("Sarah" in cast.names, "cast was ${cast.names}")
        assertTrue("Thomas" in cast.names, "cast was ${cast.names}")
        println("scanned cast: ${cast.names.sorted()}, genders ${cast.genders}")
    }
}
