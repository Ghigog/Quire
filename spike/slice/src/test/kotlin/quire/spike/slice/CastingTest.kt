package quire.spike.slice

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import quire.model.characters.Gender

/**
 * QUI-011 in spike form. The device test on 2026-08-29 heard three distinct voices and all
 * three were the wrong sex, which is what casting by bare speaker id buys you.
 */
class CastingTest {

    private val profile: VoiceProfile =
        File("../../fixtures/voices/libritts_r-f0.tsv").useLines { VoiceProfile.parse(it) }

    private fun genderOf(voice: Int): Gender = when {
        voice in profile.pool(Gender.MALE) -> Gender.MALE
        voice in profile.pool(Gender.FEMALE) -> Gender.FEMALE
        else -> Gender.UNKNOWN
    }

    @Test
    fun `the measured profile has a usable pool of each sex`() {
        assertTrue(profile.pool(Gender.MALE).size > 50, "male pool too small")
        assertTrue(profile.pool(Gender.FEMALE).size > 50, "female pool too small")
    }

    @Test
    fun `characters are cast to voices of their own sex`() {
        val casting = Casting(
            mapOf("Sarah" to Gender.FEMALE, "Thomas" to Gender.MALE),
            voiceCount = 904,
            profile = profile,
            narratorGender = Gender.FEMALE,
        )
        assertEquals(Gender.FEMALE, genderOf(casting.voiceFor("Sarah")))
        assertEquals(Gender.MALE, genderOf(casting.voiceFor("Thomas")))
        assertEquals(Gender.FEMALE, genderOf(casting.narrator))
    }

    @Test
    fun `a character never shares the narrator's voice`() {
        // Same sex as the narrator is the case that matters: it is the one distinction the
        // listener cannot afford to lose.
        val casting = Casting(
            mapOf("Sarah" to Gender.FEMALE),
            voiceCount = 904, profile = profile, narratorGender = Gender.FEMALE,
        )
        assertNotEquals(casting.narrator, casting.voiceFor("Sarah"))
    }

    @Test
    fun `two characters of the same sex still get different voices`() {
        val casting = Casting(
            mapOf("Sarah" to Gender.FEMALE, "Emma" to Gender.FEMALE, "Anne" to Gender.FEMALE),
            voiceCount = 904, profile = profile, narratorGender = Gender.MALE,
        )
        assertEquals(3, casting.cast.values.distinct().size, "voices collided: ${casting.cast}")
        assertTrue(casting.cast.values.all { genderOf(it) == Gender.FEMALE })
    }

    @Test
    fun `with no profile it still produces distinct voices`() {
        // An unscanned book, or a model nobody has profiled: worse, but never a collision.
        val casting = Casting.untyped(listOf("Sarah", "Thomas", "Mr Ashcombe"), voiceCount = 904)
        assertEquals(3, casting.cast.values.distinct().size)
        assertTrue(casting.cast.values.all { it >= 452 }, "characters crowd the narrator")
    }

    @Test
    fun `an unknown gender falls back rather than guessing a sex`() {
        val casting = Casting(
            mapOf("Stranger" to Gender.UNKNOWN),
            voiceCount = 904, profile = profile, narratorGender = Gender.FEMALE,
        )
        // No pool to draw from, so it spreads over raw ids — arbitrary sex, but a voice.
        assertTrue(casting.voiceFor("Stranger") > 0)
    }
}
