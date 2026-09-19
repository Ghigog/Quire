package quire.attribution.scan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import quire.attribution.slm.ShapeMismatch
import quire.model.characters.AgeBand
import quire.model.characters.Gender

class CharacterExtractionShapeTest {

    @Test
    fun `parses a full entry`() {
        val parsed = CharacterExtractionShape.parse(
            """[{"name":"Sarah","aliases":["Sadie"],"gender":"FEMALE","ageBand":"ADULT","traits":["quiet"]}]""",
        )
        assertEquals(
            listOf(ScannedCharacter("Sarah", listOf("Sadie"), Gender.FEMALE, AgeBand.ADULT, listOf("quiet"))),
            parsed,
        )
    }

    @Test
    fun `fields other than name are optional`() {
        val parsed = CharacterExtractionShape.parse("""[{"name":"Sarah"}]""")
        assertEquals(listOf(ScannedCharacter("Sarah")), parsed)
    }

    @Test
    fun `not an array is rejected`() {
        assertFailsWith<ShapeMismatch> { CharacterExtractionShape.parse("""{"name":"Sarah"}""") }
    }

    @Test
    fun `an entry with no name is rejected`() {
        assertFailsWith<ShapeMismatch> { CharacterExtractionShape.parse("""[{"gender":"FEMALE"}]""") }
    }

    @Test
    fun `an unrecognised gender degrades to unknown rather than failing`() {
        val parsed = CharacterExtractionShape.parse("""[{"name":"Sarah","gender":"NONBINARY"}]""")
        assertEquals(Gender.UNKNOWN, parsed.single().gender)
    }
}
