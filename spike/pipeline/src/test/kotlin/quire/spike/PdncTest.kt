package quire.spike

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The corpus loader is the part that can be silently wrong: an offset off by one still
 * produces a plausible-looking percentage, and until 2026-09-02 nothing here had a test at
 * all. These build a miniature PDNC on disk and check the offsets land where they were
 * written.
 */
class PdncTest {

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
        val (paragraphs, gold) = Pdnc.locate(File(File(root, "data"), "TestNovel"))
        val (questions, unlocatable) = quire.spike.bakeoff.Bakeoff.questions(paragraphs, gold)

        assertEquals(3, paragraphs.size)
        assertEquals(2, questions.size)
        // Two quotations with identical text land in different paragraphs. Text matching
        // cannot tell them apart; this is exactly the case that made the old scorer drop them.
        assertEquals(0, questions[0].paragraph)
        assertEquals(2, questions[1].paragraph)
        assertEquals(0, questions[0].start)
        assertEquals("Sarah", questions[0].gold)
        assertEquals(0, unlocatable)
    }

    @Test
    fun `pseudo-speakers are excluded rather than scored`() {
        val text = "\"Hello,\" said the crowd.\n"
        val root = corpus(
            text,
            "\"quoteID\",\"quoteText\",\"quoteByteSpans\",\"speaker\",\"quoteType\"\n" +
                "\"Q0\",\"Hello,\",\"[[0, 8]]\",\"_group\",\"Explicit\"\n",
        )
        val (paragraphs, gold) = Pdnc.locate(File(File(root, "data"), "TestNovel"))
        assertTrue(quire.spike.bakeoff.Bakeoff.questions(paragraphs, gold).first.isEmpty())
        assertEquals(1, gold.count { !it.scorable })
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
        val (paragraphs, gold) = Pdnc.locate(File(File(root, "data"), "TestNovel"))
        val q = quire.spike.bakeoff.Bakeoff.questions(paragraphs, gold).first.single()
        val paragraph = paragraphs[q.paragraph].unit.text
        // The wrapped paragraph is one line now, and the span still frames the speech.
        assertEquals("\"Not tonight,\"", paragraph.substring(q.start, q.end))
    }

    @Test
    fun `byte offsets survive a non-ascii corpus revision`() {
        val offsets = Pdnc.ByteOffsets("Émile said “yes”")
        // É is two bytes, so every character after it sits one further on in bytes.
        assertEquals(0, offsets.charOf(0))
        assertEquals(1, offsets.charOf(2))
        assertEquals(6, offsets.charOf(7))
    }

    @Test
    fun `an honorific with a full stop matches itself`() {
        // Regression: punctuation was stripped from the gold name and left on ours, so
        // predicted "Mr. Woodhouse" scored as a miss against gold "Mr. Woodhouse". Most of
        // Austen is honorifics, and Austen is a fifth of the corpus.
        assertTrue(Pdnc.matches("Mr. Woodhouse", "Mr. Woodhouse"))
        assertTrue(Pdnc.matches("Mrs. Weston", "Mrs Weston"))
        assertTrue(Pdnc.matches("Knightley", "Mr. Knightley"))
    }

    @Test
    fun `two people sharing a surname stay apart`() {
        // The fold must not go so far that the honorific stops carrying identity.
        assertFalse(Pdnc.matches("Mr. Bennet", "Mrs. Bennet"))
        assertFalse(Pdnc.matches("Emma", "Mr. Knightley"))
    }

    @Test
    fun `an alias the novel uses is the same character`() {
        // PDNC's own table says Charlotte Lucas is also "Miss Lucas". The strings share no
        // word, so string matching alone scored a correctly-read line as a wrong voice.
        val identity = Pdnc.Identity(listOf(
            Pdnc.GoldCharacter("Charlotte Lucas", setOf("Charlotte Lucas", "Miss Lucas", "Charlotte"), "F", "intermediate"),
            Pdnc.GoldCharacter("Mr. Darcy", setOf("Mr. Darcy", "Darcy"), "M", "major"),
        ))
        assertTrue(identity.matches("Miss Lucas", "Charlotte Lucas"))
        assertTrue(identity.matches("Charlotte", "Charlotte Lucas"))
        assertFalse(identity.matches("Mr. Darcy", "Charlotte Lucas"))
    }

    @Test
    fun `an alias two characters claim is not trusted`() {
        // PDNC's sets are hand-made and leak: Charlotte Lucas's list carries "Lady Lucas",
        // who is a character in her own right. Crediting one for the other would invent
        // precision, so an ambiguous alias is dropped and only string matching remains.
        val identity = Pdnc.Identity(listOf(
            Pdnc.GoldCharacter("Charlotte Lucas", setOf("Charlotte Lucas", "Lady Lucas"), "F", "intermediate"),
            Pdnc.GoldCharacter("Lady Lucas", setOf("Lady Lucas"), "F", "minor"),
        ))
        assertFalse(identity.matches("Lady Lucas", "Charlotte Lucas"))
        assertTrue(identity.matches("Charlotte Lucas", "Charlotte Lucas"))
    }

    @Test
    fun `python span literals parse`() {
        assertEquals(listOf(2309 to 2585, 2600 to 2610), Pdnc.spans("[[2309, 2585], [2600, 2610]]"))
        assertEquals(emptyList(), Pdnc.spans(""))
    }
}
