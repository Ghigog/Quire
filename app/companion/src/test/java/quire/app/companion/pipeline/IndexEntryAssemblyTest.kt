package quire.app.companion.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import quire.model.AttributionResult
import quire.model.Kind
import quire.model.Paragraph
import quire.model.Tier

class IndexEntryAssemblyTest {

    @Test
    fun `a tagged quotation carries its speaker, the rest reads as narration`() {
        val paragraph = Paragraph("ch0#p0", "\"I know,\" said Sarah.", chapterIndex = 2, index = 0)
        val attributed = listOf(
            AttributionResult("ch0#p0#s0", "\"I know,\"", Kind.DIALOGUE, "Sarah", 0.95, Tier.HEURISTIC),
            AttributionResult("ch0#p0#s1", "said Sarah.", Kind.NARRATION, null, 0.0, Tier.NARRATOR),
        )

        val entries = IndexEntryAssembly.assemble(listOf(paragraph), attributed)

        assertEquals(1, entries.size, "one sentence, even though it holds two segments")
        val entry = entries.single()
        assertEquals(2, entry.chapter)
        assertEquals(2, entry.spans.size)
        assertEquals("Sarah", entry.spans.first { it.kind == Kind.DIALOGUE }.speakerId)
        assertEquals(null, entry.spans.first { it.kind == Kind.NARRATION }.speakerId)
    }

    @Test
    fun `a paragraph with no attributed segments falls back to whole-sentence narration`() {
        val paragraph = Paragraph("ch0#p1", "The rain fell all night.", chapterIndex = 0, index = 1)

        val entries = IndexEntryAssembly.assemble(listOf(paragraph), emptyList())

        val span = entries.single().spans.single()
        assertEquals(Kind.NARRATION, span.kind)
        assertEquals(null, span.speakerId)
        assertEquals(0, span.start)
        assertEquals(paragraph.text.length, span.end)
    }
}
