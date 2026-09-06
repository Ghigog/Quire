package quire.voice.design

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.model.characters.AgeBand
import quire.model.characters.Character
import quire.model.characters.Gender
import quire.model.characters.VoiceSource
import quire.voice.foundry.SpeakerProfile

class VoiceDesignerTest {

    private val profile: SpeakerProfile =
        File("../../fixtures/voices/libritts_r-f0.tsv").useLines { SpeakerProfile.parse(it) }
    private val designer = VoiceDesigner(profile)

    private fun character(gender: Gender = Gender.FEMALE, ageBand: AgeBand = AgeBand.ADULT) = Character(
        id = "sarah", displayName = "Sarah", gender = gender, ageBand = ageBand,
        traits = listOf("nervous", "talkative", "extra trait dropped"),
    )

    @Test
    fun `too few confidently-attributed lines produces no descriptor`() {
        val voice = designer.design(character(), explicitLines = listOf("Hello.", "Wait."))
        assertNull(voice)
    }

    @Test
    fun `enough lines produces a descriptor grounded in the measured range`() {
        val lines = listOf("I said no.", "Please, wait a moment.", "It was raining that day.")
        val voice = designer.design(character(Gender.FEMALE, AgeBand.ADULT), lines)

        assertNotNull(voice)
        assertTrue(
            voice.targetF0Hz!! >= profile.medianF0(Gender.FEMALE)!! - 50 &&
                voice.targetF0Hz!! <= profile.medianF0(Gender.FEMALE)!! + 50,
            "targetF0Hz ${voice.targetF0Hz} implausibly far from the measured female median",
        )
        assertEquals(VoiceSource.AUTO, voice.source)
        assertTrue(voice.description!!.contains("woman"))
        assertTrue(voice.description!!.contains("nervous"))
    }

    @Test
    fun `an elder is pitched below an adult of the same gender`() {
        val lines = listOf("I said no.", "Please, wait a moment.", "It was raining that day.")
        val adult = designer.design(character(Gender.MALE, AgeBand.ADULT), lines)!!
        val elder = designer.design(character(Gender.MALE, AgeBand.ELDER), lines)!!
        assertTrue(elder.targetF0Hz!! < adult.targetF0Hz!!)
    }

    @Test
    fun `length scale stays within the safe band`() {
        val shortLines = listOf("No.", "Wait.", "Stop.")
        val voice = designer.design(character(), shortLines)!!
        assertTrue(voice.lengthScale!! in 0.85..1.2)
    }

    @Test
    fun `only up to two traits reach the description`() {
        val lines = listOf("I said no.", "Please, wait a moment.", "It was raining that day.")
        val voice = designer.design(character(), lines)!!
        assertTrue(!voice.description!!.contains("extra trait dropped"))
    }
}
