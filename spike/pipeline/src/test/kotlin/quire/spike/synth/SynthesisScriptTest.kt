package quire.spike.synth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.spike.ParagraphUnit

class SynthesisScriptTest {

    private fun unit(locator: String, text: String, chapter: Int, index: Int) =
        ParagraphUnit(locator = locator, text = text, chapterIndex = chapter, index = index)

    @Test
    fun `only the requested chapter's pieces are kept`() {
        val units = listOf(
            unit("book#p0", "\"I know,\" said Sarah.", chapter = 0, index = 0),
            unit("book#p1", "Thomas said, \"It is late.\"", chapter = 1, index = 1),
        )

        val script = SynthesisScript.build("book", units, chapterIndex = 1)

        assertEquals(1, script.chapter)
        assertTrue(script.pieces.all { it.text.contains("It is late") || it.text.contains("Thomas") })
        assertTrue(script.pieces.none { it.text.contains("Sarah") })
    }

    @Test
    fun `a tagged line is attributed and joins the cast`() {
        val units = listOf(unit("book#p0", "\"I know,\" said Sarah.", chapter = 0, index = 0))

        val script = SynthesisScript.build("book", units, chapterIndex = 0)

        assertEquals(listOf("Sarah"), script.cast)
        val quote = script.pieces.first { it.text.startsWith("\"") }
        assertEquals("Sarah", quote.speaker)
    }

    @Test
    fun `an unresolved chapter still produces narration, not an error`() {
        // No speech tag Tier 1 can bootstrap a roster from: everything stays unnamed, and
        // architecture.md's rule is that this falls to the narrator rather than blocking.
        val units = listOf(unit("book#p0", "The rain kept on.", chapter = 0, index = 0))

        val script = SynthesisScript.build("book", units, chapterIndex = 0)

        assertTrue(script.cast.isEmpty())
        assertTrue(script.pieces.isNotEmpty())
        assertTrue(script.pieces.all { it.speaker == null })
    }

    @Test
    fun `an empty chapter writes valid json`() {
        val script = SynthesisScript.Script("book", 0, emptyList(), emptyList())
        val out = kotlin.io.path.createTempFile(suffix = ".json").toFile()
        SynthesisScript.write(script, out)
        val text = out.readText()
        assertTrue(text.contains("\"cast\": []"))
        assertTrue(text.contains("\"pieces\": ["))
    }
}
