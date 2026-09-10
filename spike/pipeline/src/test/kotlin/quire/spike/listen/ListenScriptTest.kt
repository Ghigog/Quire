package quire.spike.listen

import kotlin.test.Test
import kotlin.test.assertEquals
import quire.spike.bakeoff.Question

/**
 * Cutting a paragraph into voices. Everything here was found by reading the rendered script
 * rather than by reasoning about it, which is the argument for the script being a file a
 * person can read at all.
 */
class ListenScriptTest {

    private fun cut(text: String, vararg quotes: Pair<Int, Int>) =
        ListenScript.pieces(
            paragraph = 0,
            text = text,
            quotations = quotes.mapIndexed { i, (s, e) -> Question("q$i", 0, s, e, "Implicit", "unused") },
        ).map { (if (it.quotationId == null) "narration" else "quote") + ": " + it.text }

    @Test
    fun `a beat and a line are separate voices`() {
        val text = "Marcus set his mug down. \"We don't have enough time.\""
        assertEquals(
            listOf("narration: Marcus set his mug down.", "quote: \"We don't have enough time.\""),
            cut(text, 26 to text.length - 1),
        )
    }

    @Test
    fun `the quotation marks go with the quotation`() {
        // PDNC's spans stop inside the marks. Cut naively, the opening one trails the narration
        // and the closing one leads the next piece — heard as a stray mark read aloud, which is
        // exactly how this was found.
        val text = "\"Well, I guess you had better be quiet,\" she simply observed."
        assertEquals(
            listOf("quote: \"Well, I guess you had better be quiet,\"", "narration: she simply observed."),
            cut(text, 1 to 39),
        )
    }

    @Test
    fun `a fragment holding no words is not voiced`() {
        // A lone quote mark reaches the synthesiser as a noise rather than as speech.
        val text = "\"Yes.\" \"No.\""
        assertEquals(listOf("quote: \"Yes.\"", "quote: \"No.\""), cut(text, 1 to 5, 8 to 11))
    }

    @Test
    fun `two speakers in one paragraph keep their own lines`() {
        val text = "\"I know,\" said Sarah. \"You do not,\" said Thomas."
        assertEquals(
            listOf(
                "quote: \"I know,\"",
                "narration: said Sarah.",
                "quote: \"You do not,\"",
                "narration: said Thomas.",
            ),
            cut(text, 1 to 8, 23 to 34),
        )
    }
}
