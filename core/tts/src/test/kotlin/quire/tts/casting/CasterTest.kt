package quire.tts.casting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import quire.model.characters.AgeBand
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender

class CasterTest {

    private fun character(
        id: String,
        gender: Gender = Gender.UNKNOWN,
        ageBand: AgeBand = AgeBand.UNKNOWN,
        lineCount: Int = 0,
    ) = Character(id = id, displayName = id, gender = gender, ageBand = ageBand, lineCount = lineCount)

    private fun manifest(vararg characters: Character, narrator: Character = character("narrator")) =
        CharacterManifest(
            schemaVersion = CharacterManifest.VERSION, bookId = "book-1", generatedAt = 0L,
            narrator = narrator, characters = characters.toList(),
        )

    @Test
    fun `every character is cast without user input`() {
        val sarah = character("Sarah", Gender.FEMALE)
        val thomas = character("Thomas", Gender.MALE)
        val cast = Caster.cast(manifest(sarah, thomas), coPresence = emptyList(), pool = VoicePool.flat((0..9).toList()))

        assertTrue("Sarah" in cast.voices)
        assertTrue("Thomas" in cast.voices)
        assertTrue(cast.narratorVoiceId >= 0)
    }

    @Test
    fun `casting is deterministic`() {
        val m = manifest(character("Sarah", Gender.FEMALE, lineCount = 5), character("Thomas", Gender.MALE, lineCount = 2))
        val pool = VoicePool.flat((0..9).toList())

        val first = Caster.cast(m, emptyList(), pool)
        val second = Caster.cast(m, emptyList(), pool)

        assertEquals(first, second)
    }

    @Test
    fun `co-present characters are never assigned the same voice and rate`() {
        // A narrow pool of two, three characters who all share one scene — the pool cannot
        // give everyone a distinct id, so the third must be told apart by rate instead.
        val a = character("A", Gender.FEMALE, lineCount = 3)
        val b = character("B", Gender.FEMALE, lineCount = 2)
        val c = character("C", Gender.FEMALE, lineCount = 1)
        val cast = Caster.cast(
            manifest(a, b, c),
            coPresence = listOf(setOf("A", "B", "C")),
            pool = VoicePool.flat(listOf(10, 20)),
        )

        val assignments = listOf(cast.voices.getValue("A"), cast.voices.getValue("B"), cast.voices.getValue("C"))
        val pairs = assignments.map { it.voiceId to it.rate }
        assertEquals(pairs.size, pairs.distinct().size, "every co-present pair must be distinct: $pairs")
    }

    @Test
    fun `characters who never share a scene may reuse a voice id`() {
        // A pool of one: with no co-presence constraint there is nothing to differentiate.
        val cast = Caster.cast(
            manifest(character("A", Gender.MALE), character("B", Gender.MALE)),
            coPresence = emptyList(),
            pool = VoicePool.flat(listOf(7)),
        )

        assertEquals(7, cast.voices.getValue("A").voiceId)
        assertEquals(7, cast.voices.getValue("B").voiceId)
    }

    @Test
    fun `gender and age select the matching pool where one exists`() {
        val pool = VoicePool(
            groups = mapOf(
                (Gender.MALE to AgeBand.ELDER) to listOf(100),
                (Gender.MALE to AgeBand.ADULT) to listOf(200),
                (Gender.FEMALE to AgeBand.ADULT) to listOf(300),
            ),
            fallback = listOf(999),
        )
        val elder = character("Grandfather", Gender.MALE, AgeBand.ELDER)
        val cast = Caster.cast(manifest(elder), emptyList(), pool)

        assertEquals(100, cast.voices.getValue("Grandfather").voiceId)
    }

    @Test
    fun `an unmatched age band falls back to the rest of that gender's pool`() {
        val pool = VoicePool(
            groups = mapOf((Gender.MALE to AgeBand.ADULT) to listOf(200)),
            fallback = listOf(999),
        )
        // No MALE/CHILD group exists; the pool degrades to any MALE pool rather than to fallback.
        val boy = character("Boy", Gender.MALE, AgeBand.CHILD)
        val cast = Caster.cast(manifest(boy), emptyList(), pool)

        assertEquals(200, cast.voices.getValue("Boy").voiceId)
    }

    @Test
    fun `a user override survives a rescan`() {
        val m = manifest(character("Sarah", Gender.FEMALE, lineCount = 5))
        val pool = VoicePool.flat((0..9).toList())
        val auto = Caster.cast(m, emptyList(), pool)

        val overridden = auto.copy(
            voices = auto.voices + ("Sarah" to VoiceAssignment(voiceId = 42, source = AssignmentSource.USER)),
        )

        val rescanned = Caster.cast(m, emptyList(), pool, previous = overridden)

        assertEquals(VoiceAssignment(42, source = AssignmentSource.USER), rescanned.voices.getValue("Sarah"))
    }

    @Test
    fun `priority by line count claims the best-spread slots first`() {
        // Three characters sharing a scene and a pool of three: the busiest two should not
        // be the ones pushed to a rate-shifted fallback.
        val lead = character("Lead", Gender.FEMALE, lineCount = 100)
        val second = character("Second", Gender.FEMALE, lineCount = 50)
        val walkOn = character("WalkOn", Gender.FEMALE, lineCount = 1)
        val cast = Caster.cast(
            manifest(lead, second, walkOn),
            coPresence = listOf(setOf("Lead", "Second", "WalkOn")),
            pool = VoicePool.flat(listOf(1, 2)),
        )

        assertEquals(1.0, cast.voices.getValue("Lead").rate)
        assertEquals(1.0, cast.voices.getValue("Second").rate)
        assertNotEquals(1.0, cast.voices.getValue("WalkOn").rate)
    }
}
