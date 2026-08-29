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
    fun `a character of the narrator's sex is far enough away in pitch to hear`() {
        // Asserting only that the ids differ is not enough, and passed while shipping a
        // narrator and a Sarah that were different speakers at an identical 188.5 Hz. Two
        // voices of one sex at one pitch are one voice to a listener.
        val casting = Casting(
            mapOf("Sarah" to Gender.FEMALE),
            voiceCount = 904, profile = profile, narratorGender = Gender.FEMALE,
        )
        val sarah = casting.voiceFor("Sarah")
        assertNotEquals(casting.narrator, sarah)

        val gap = kotlin.math.abs(profile.f0Of(sarah)!! - profile.f0Of(casting.narrator)!!)
        assertTrue(gap >= Casting.GUARD_HZ, "narrator and Sarah are only %.1f Hz apart".format(gap))
    }

    @Test
    fun `two characters of the same sex still get different voices`() {
        val casting = Casting(
            mapOf("Sarah" to Gender.FEMALE, "Emma" to Gender.FEMALE, "Anne" to Gender.FEMALE),
            voiceCount = 904, profile = profile, narratorGender = Gender.MALE,
        )
        assertEquals(3, casting.cast.values.distinct().size, "voices collided: ${casting.cast}")
        assertTrue(casting.cast.values.all { genderOf(it) == Gender.FEMALE })

        // And audibly apart from each other, not merely different integers.
        val pitches = casting.cast.values.map { profile.f0Of(it)!! }.sorted()
        val closest = pitches.zipWithNext { a, b -> b - a }.min()
        assertTrue(closest >= 5.0, "two of the three are only %.1f Hz apart".format(closest))
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
