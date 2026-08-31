package quire.attribution

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression for the 157-character roster: importing a real novel filled the cast with
 * words that are capitalised for a reason other than being someone's name.
 */
class RosterJunkTest {

    private fun scan(lines: List<String>) =
        Roster.scan(lines.mapIndexed { i, text -> "p$i" to text })

    /** Enough repetitions that only the rule under test can keep a name out. */
    private fun repeated(vararg lines: String) =
        scan((1..Roster.ADJACENCY_MIN).flatMap { lines.asList() })

    @Test
    fun `a word that opens a line of speech is not a character`() {
        // The context around one quotation includes the quotation beside it, and its first
        // word is capitalised because the speech starts there.
        val cast = repeated(
            "\"Absolutely.\" \"And another thing.\"",
            "\"Dammit.\" \"Quite so.\"",
        )
        assertEquals(emptySet(), cast.names)
    }

    @Test
    fun `a real name beside those quotes still survives`() {
        val cast = repeated(
            "\"Absolutely.\" Geralt leaned on the rail. \"And another thing.\"",
        )
        assertEquals(setOf("Geralt"), cast.names)
    }
}
