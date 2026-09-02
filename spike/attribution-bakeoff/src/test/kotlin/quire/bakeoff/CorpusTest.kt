package quire.bakeoff

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The corpus loader is the part of the harness that can be silently wrong: an offset off by
 * one still produces a plausible-looking percentage. These build a miniature PDNC on disk
 * and check the offsets land where they were written.
 */
class CorpusTest {

    private fun corpus(text: String, quotations: String): File {
        val root = createTempDirectory()
        File(root, "PDNC-Novel-Index.csv").writeText(
            "Novel Code,Novel Title,Narrative Person,Translator Code,Genre," +
                "Year of First Publication,Folder Name\n" +
                "TEST,A Test Novel,3.0,,literary,1900.0,TestNovel\n")
        val dir = File(File(root, "data"), "TestNovel").apply { mkdirs() }
        File(dir, "novel_text.txt").writeText(text)
        File(dir, "quotation_info.csv").writeText(quotations)
        return root
    }

    private fun createTempDirectory() =
        File.createTempFile("pdnc", "").let { it.delete(); it.mkdirs(); it }

    @Test
    fun `a quotation is located by byte span, not by its text`() {
        val text = "\"Good morning,\" said Sarah.\n\nHe said nothing at all.\n\n\"Good morning,\" said Thomas.\n"
        val open = text.indexOf('"')
        val close = text.indexOf('.', open)
        val second = text.lastIndexOf("\"Good")
        val root = corpus(
            text,
            "\"quoteID\",\"quoteText\",\"quoteByteSpans\",\"speaker\",\"quoteType\"\n" +
                "\"Q0\",\"Good morning,\",\"[[$open, ${close}]]\",\"Sarah\",\"Explicit\"\n" +
                "\"Q1\",\"Good morning,\",\"[[$second, ${second + 15}]]\",\"Thomas\",\"Explicit\"\n",
        )
        val novel = Corpus.load(root, Corpus.index(root).single())

        assertEquals(3, novel.paragraphs.size)
        assertEquals(2, novel.questions.size)
        // Two quotations with identical text land in different paragraphs. Text matching
        // cannot tell them apart; this is exactly the case that made the old scorer drop them.
        assertEquals(0, novel.questions[0].paragraph)
        assertEquals(2, novel.questions[1].paragraph)
        assertEquals(0, novel.questions[0].start)
        assertEquals("Sarah", novel.questions[0].gold)
        assertEquals(0, novel.unlocatable)
    }

    @Test
    fun `pseudo-speakers are excluded rather than scored`() {
        val text = "\"Hello,\" said the crowd.\n"
        val root = corpus(
            text,
            "\"quoteID\",\"quoteText\",\"quoteByteSpans\",\"speaker\",\"quoteType\"\n" +
                "\"Q0\",\"Hello,\",\"[[0, 8]]\",\"_group\",\"Explicit\"\n",
        )
        val novel = Corpus.load(root, Corpus.index(root).single())
        assertTrue(novel.questions.isEmpty())
        assertEquals(1, novel.unscorable)
    }

    @Test
    fun `line breaks inside a paragraph keep every offset`() {
        val text = "He turned.\n\"Not tonight,\"\nshe said.\n\nAnd left.\n"
        val at = text.indexOf('"')
        val root = corpus(
            text,
            "\"quoteID\",\"quoteText\",\"quoteByteSpans\",\"speaker\",\"quoteType\"\n" +
                "\"Q0\",\"Not tonight,\",\"[[$at, ${at + 14}]]\",\"Sarah\",\"Anaphoric\"\n",
        )
        val novel = Corpus.load(root, Corpus.index(root).single())
        val q = novel.questions.single()
        val paragraph = novel.paragraphs[q.paragraph].text
        // The wrapped paragraph is one line now, and the span still frames the speech.
        assertEquals("\"Not tonight,\"", paragraph.substring(q.start, q.end))
    }

    @Test
    fun `byte offsets survive a non-ascii corpus revision`() {
        val offsets = Corpus.ByteOffsets("Émile said “yes”")
        // É is two bytes, so every character after it sits one further on in bytes.
        assertEquals(0, offsets.charOf(0))
        assertEquals(1, offsets.charOf(2))
        assertEquals(6, offsets.charOf(7))
    }

    @Test
    fun `python span literals parse`() {
        assertEquals(listOf(2309 to 2585, 2600 to 2610), Corpus.spans("[[2309, 2585], [2600, 2610]]"))
        assertEquals(emptyList(), Corpus.spans(""))
    }
}
