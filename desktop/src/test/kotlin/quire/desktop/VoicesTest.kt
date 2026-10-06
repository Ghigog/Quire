package quire.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender
import quire.voice.foundry.SpeakerProfile

class VoicesTest {

    /** Four real speakers, two per gender, so "nearest by pitch" has something to choose. */
    private val profile = SpeakerProfile(
        listOf(
            SpeakerProfile.Voice(0, 100.0, Gender.MALE),
            SpeakerProfile.Voice(1, 120.0, Gender.MALE),
            SpeakerProfile.Voice(2, 200.0, Gender.FEMALE),
            SpeakerProfile.Voice(3, 220.0, Gender.FEMALE),
        ),
    )

    private fun manifest(vararg people: Pair<String, Gender>) = CharacterManifest(
        schemaVersion = CharacterManifest.VERSION,
        bookId = "test",
        generatedAt = 0,
        narrator = Character("narrator", "Narrator", gender = Gender.NEUTRAL),
        characters = people.map { (name, gender) ->
            Character(id = name, displayName = name, gender = gender, confidence = 1.0)
        },
    )

    @Test
    fun `every character and the narrator get a speaker the engine actually has`() {
        val voices = Voices(profile).assign(
            manifest("Sarah" to Gender.FEMALE, "Thomas" to Gender.MALE),
        )

        assertEquals(setOf("Sarah", "Thomas", Voices.NARRATOR), voices.keys)
        for ((who, speaker) in voices) {
            assertTrue(speaker in 0..3, "$who got speaker $speaker, outside the model's four")
        }
    }

    @Test
    fun `a character with no descriptor still lands in their own gender's pool`() {
        val voices = Voices(profile).assign(
            manifest("Sarah" to Gender.FEMALE, "Thomas" to Gender.MALE),
        )

        assertTrue(voices.getValue("Sarah") in setOf(2, 3))
        assertTrue(voices.getValue("Thomas") in setOf(0, 1))
    }

    @Test
    fun `two characters of one gender do not share a voice`() {
        val voices = Voices(profile).assign(
            manifest("Sarah" to Gender.FEMALE, "Ellen" to Gender.FEMALE, "Thomas" to Gender.MALE),
        )
        val cast = listOf("Sarah", "Ellen", "Thomas").map { voices.getValue(it) }

        assertEquals(cast.size, cast.toSet().size, "two characters were given the same speaker: $cast")
    }

    @Test
    fun `a full name and its short form are one voice, not two`() {
        // Found on The Sign of the Four: the scan yields both, and voice them separately.
        val voices = Voices(profile).assign(
            manifest(
                "Sherlock Holmes" to Gender.MALE,
                "Holmes" to Gender.MALE,
                "Watson" to Gender.MALE,
            ),
        )

        assertEquals(
            voices.getValue("Sherlock Holmes"),
            voices.getValue("Holmes"),
            "the same man was cast twice",
        )
        assertTrue(voices.getValue("Watson") != voices.getValue("Holmes"))
    }

    @Test
    fun `a name that merely ends in another name is left alone`() {
        // `Sherman` ends with `herman`; `s|herman` is not a word start, so they stay apart.
        val voices = Voices(profile).assign(
            manifest("Sherman" to Gender.MALE, "Herman" to Gender.MALE),
        )

        assertTrue(
            voices.getValue("Sherman") != voices.getValue("Herman"),
            "two different men were collapsed into one voice",
        )
    }

    @Test
    fun `a merged group takes a gender from whichever member knows one`() {
        // The scan gives `Jonathan Small` a full name and no gender, and `Small` a gender.
        val voices = Voices(profile).assign(
            manifest("Jonathan Small" to Gender.MALE, "Small" to Gender.UNKNOWN),
        )

        assertEquals(voices.getValue("Jonathan Small"), voices.getValue("Small"))
        assertTrue(voices.getValue("Small") in setOf(0, 1), "did not land in the male pool")
    }

    @Test
    fun `the narrator does not take a name that is already cast`() {
        // The narrator reads every line nobody claimed, which is where a listener most needs
        // to tell voices apart — so it must not collide with a character when it need not.
        val manifest = manifest("Sarah" to Gender.FEMALE, "Thomas" to Gender.MALE)
        val voices = Voices(profile).assign(manifest)
        val castSpeakers = manifest.characters.map { voices.getValue(it.id) }.toSet()

        assertTrue(voices.getValue(Voices.NARRATOR) !in castSpeakers)
    }
}
