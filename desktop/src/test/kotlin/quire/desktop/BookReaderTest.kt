package quire.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import quire.attribution.Conversation
import quire.model.AttributionResult
import quire.model.Kind
import quire.model.Tier

class BookReaderTest {

    private fun line(locator: String, text: String, speaker: String?) = AttributionResult(
        locator = locator,
        text = text,
        kind = Kind.DIALOGUE,
        speakerId = speaker,
        confidence = if (speaker == null) 0.0 else 0.95,
        tier = if (speaker == null) Tier.NONE else Tier.HEURISTIC,
    )

    /** Chapter 0 tags Ellen once; chapter 1 opens on the same shape with nobody on the floor. */
    private val tier1 = listOf(
        line("a.xhtml#p0#s0", "\"You will miss it,\" said Ellen.", "Ellen"),
        line("a.xhtml#p1#s0", "\"There is another at nine.\"", null),
        line("b.xhtml#p0#s0", "\"You are late.\"", null),
        line("b.xhtml#p1#s0", "\"I know.\"", null),
    )

    private val chapterOfParagraph = mapOf(
        "a.xhtml#p0" to 0, "a.xhtml#p1" to 0,
        "b.xhtml#p0" to 1, "b.xhtml#p1" to 1,
    )

    private val cast = listOf("Ellen", "Robert", "Dana")

    @Test
    fun `the whole-book pass is what puts the previous chapter's speaker on the next one`() {
        // This is the behaviour being corrected, asserted so the correction stays a choice
        // rather than an accident: Holmes was last tagged in Chapter I, so Chapter II's
        // opening reply — Miss Morstan's — came back as his.
        val wholeBook = Conversation.resolve(tier1, cast = cast)

        assertEquals("Ellen", wholeBook[2].speakerId)
        assertEquals("revert to Ellen", wholeBook[2].evidence)
    }

    @Test
    fun `a chapter opens with nobody on the floor`() {
        val perChapter = BookReader.turnTakingByChapter(tier1, chapterOfParagraph, cast)

        assertNull(perChapter[2].speakerId, "chapter 2 inherited chapter 1's speaker")
        assertNull(perChapter[3].speakerId)
    }

    @Test
    fun `a chapter still gets its own turn-taking`() {
        val perChapter = BookReader.turnTakingByChapter(tier1, chapterOfParagraph, cast)

        // No regression to "turn-taking is off": chapter 0 is unchanged.
        assertEquals("Ellen", perChapter[0].speakerId)
    }

    @Test
    fun `segments keep their reading order across the chapter split`() {
        val perChapter = BookReader.turnTakingByChapter(tier1, chapterOfParagraph, cast)

        assertEquals(tier1.map { it.locator }, perChapter.map { it.locator })
    }
}
