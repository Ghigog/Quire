package quire.spike.bakeoff

import quire.spike.Pdnc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HoldoutsTest {

    private fun meta(folder: String, person: Int = 3, translated: Boolean = false, genre: String = "literary") =
        Pdnc.NovelMeta(folder, folder, person, translated, genre, 1900)

    @Test
    fun `each axis picks the novels it asks for`() {
        val index = listOf(
            meta("Emma"),
            meta("TheGambler", person = 1, translated = true),
            meta("TheSignOfTheFour", person = 1, genre = "crime"),
            meta("TheInvisibleMan", genre = "scifi"),
        )
        val byAxis = Holdouts.byAxis(index)
        assertEquals(listOf("TheGambler"), byAxis.getValue(Holdouts.Axis.TRANSLATION).map { it.folder })
        assertEquals(
            listOf("TheSignOfTheFour", "TheInvisibleMan"),
            byAxis.getValue(Holdouts.Axis.ACTION_BEATS).map { it.folder },
        )
        assertEquals(
            listOf("TheGambler", "TheSignOfTheFour"),
            byAxis.getValue(Holdouts.Axis.FIRST_PERSON).map { it.folder },
        )
    }

    @Test
    fun `a novel can sit on two axes and is held out once`() {
        val gambler = meta("TheGambler", person = 1, translated = true)
        assertEquals(2, Holdouts.axisOf(gambler).size)
        assertTrue(Holdouts.heldOut(gambler))
        assertEquals(emptyList(), Holdouts.headline(listOf(gambler)))
    }

    @Test
    fun `the headline set is everything no axis claimed`() {
        val index = listOf(meta("Emma"), meta("Persuasion"), meta("WinnieThePooh", person = 1))
        assertEquals(listOf("Emma", "Persuasion"), Holdouts.headline(index).map { it.folder })
    }
}
