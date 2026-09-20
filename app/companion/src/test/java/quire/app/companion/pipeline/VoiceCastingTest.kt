package quire.app.companion.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import quire.model.AttributionResult
import quire.model.Kind
import quire.model.Tier
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender
import quire.model.characters.Voice
import quire.model.characters.VoiceSource

class VoiceCastingTest {

    private fun manifestOf(vararg ids: String) = CharacterManifest(
        schemaVersion = CharacterManifest.VERSION,
        bookId = "book-1",
        generatedAt = 0L,
        narrator = Character(id = "Narrator", displayName = "Narrator"),
        characters = ids.map { Character(id = it, displayName = it, gender = Gender.FEMALE) },
    )

    private fun explicitLine(speaker: String, text: String) =
        AttributionResult("loc#s0", text, Kind.DIALOGUE, speaker, 0.95, Tier.HEURISTIC, evidence = "speech tag")

    @Test
    fun `three or more explicit lines earns a voice descriptor`() {
        val manifest = manifestOf("Sarah")
        val lines = listOf("one", "two", "three").map { explicitLine("Sarah", it) }

        val cast = VoiceCasting().assign(manifest, lines)

        assertNotNull(cast.characters.single().voice)
    }

    @Test
    fun `too few explicit lines leaves the voice undesigned`() {
        val manifest = manifestOf("Sarah")
        val lines = listOf(explicitLine("Sarah", "one"), explicitLine("Sarah", "two"))

        val cast = VoiceCasting().assign(manifest, lines)

        assertNull(cast.characters.single().voice)
    }

    @Test
    fun `a scene-inference line never counts as explicit`() {
        val manifest = manifestOf("Sarah")
        val sceneInferred = AttributionResult("loc#s0", "text", Kind.DIALOGUE, "Sarah", 0.90, Tier.SCENE)
        val lines = List(5) { sceneInferred }

        val cast = VoiceCasting().assign(manifest, lines)

        assertNull(cast.characters.single().voice)
    }

    @Test
    fun `an existing user-designed voice is never overwritten`() {
        val manifest = manifestOf("Sarah")
        val userVoiced = manifest.copy(
            characters = manifest.characters.map {
                it.copy(voice = Voice(description = "a specific voice", source = VoiceSource.USER))
            },
        )
        val lines = listOf("one", "two", "three").map { explicitLine("Sarah", it) }

        val cast = VoiceCasting().assign(userVoiced, lines)

        assertEquals("a specific voice", cast.characters.single().voice?.description)
    }
}
