package quire.desktop

import kotlin.math.abs
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender
import quire.voice.foundry.Foundry
import quire.voice.foundry.QualityList
import quire.voice.foundry.SpeakerProfile

/**
 * QUI-046: which of the engine's 904 speakers reads each character.
 *
 * The decision this makes is the same one ADR-0009 describes, reduced to the part a loaded
 * VITS model can actually honour.
 *
 * **What it cannot do, and why.** [Foundry.plan] names *two* speakers and a fraction between
 * them; realising the fraction means reading the model's `emb_g.weight`, interpolating the
 * row and writing it back. No sherpa-onnx binding — Kotlin or Java — exposes that, which is
 * exactly the gap ADR-0009 leaves open and QUI-010 owns. So the plan degrades to `parentA`:
 * the nearer of two *measured* real speakers, rather than a synthesised point between them.
 * The voice is real and distinct per character. It is simply not yet the interpolated one.
 *
 * A character with no descriptor (fewer than [quire.voice.design.VoiceDesigner.MIN_LINES]
 * confidently-tagged lines) falls back to their gender's lowest-pitched speaker, which is
 * stable per gender and therefore at least consistent for that character across a book.
 */
class Voices(
    private val profile: SpeakerProfile,
    private val quality: QualityList = QualityList.EMPTY,
) {

    /** Character id to engine speaker id, plus the narrator under [NARRATOR]. */
    fun assign(manifest: CharacterManifest): Map<String, Int> {
        val taken = mutableSetOf<Int>()
        val voices = LinkedHashMap<String, Int>()
        for (group in samePeople(manifest.characters)) {
            val speaker = speakerFor(group, taken)
            taken += speaker
            for (character in group) voices[character.id] = speaker
        }
        voices[NARRATOR] = narratorSpeaker(taken)
        return voices
    }

    /**
     * Characters who are one person, so they get one voice.
     *
     * The scan does not always collapse a full name and its short form: reading *The Sign of
     * the Four* yields both `Sherlock Holmes` and `Holmes`, both `Athelney Jones` and
     * `Jones`, and gives each its own manifest entry. Casting them separately is exactly the
     * failure PRD §3.1 exists to prevent — the same man speaking in two voices is heard
     * immediately, and it is the same character being wrong twice.
     *
     * A name that **nest at a word boundary** inside a longer one is treated as the same
     * person. The boundary matters: `Sherman` ends with `herman`, but `s|herman` is not a
     * word start, so the two stay separate. Merging is also the safer error where the text
     * is genuinely ambiguous — one voice that is sometimes right beats two that are each
     * sometimes wrong.
     */
    private fun samePeople(characters: List<Character>): List<List<Character>> {
        val remaining = characters.sortedByDescending { it.displayName.length }.toMutableList()
        val groups = mutableListOf<List<Character>>()
        while (remaining.isNotEmpty()) {
            val head = remaining.removeAt(0)
            val key = head.displayName.lowercase()
            val nested = remaining.filter { isNameInside(it.displayName, key) }
            remaining.removeAll(nested)
            groups += listOf(head) + nested
        }
        return groups
    }

    /** Is [name] the tail of [container], starting at a word boundary? */
    private fun isNameInside(name: String, container: String): Boolean {
        val tail = name.lowercase()
        if (tail.isEmpty() || tail.length >= container.length) return false
        if (!container.endsWith(tail)) return false
        val before = container[container.length - tail.length - 1]
        return !before.isLetterOrDigit()
    }

    /**
     * The group's speaker, preferring the foundry's descriptor and falling back to the
     * nearest free speaker in the group's gender.
     *
     * **Two characters must not share a voice while the model has a spare.** 904 speakers
     * is not a budget to spend, and a book where two people sound identical is worse than
     * one where a minor character is pitched further from their descriptor than ideal —
     * telling voices apart is the entire feature.
     */
    private fun speakerFor(group: List<Character>, taken: Set<Int>): Int {
        val gender = group.map { it.gender }
            .firstOrNull { it == Gender.MALE || it == Gender.FEMALE }
            ?: group.first().gender

        val preferred = group.firstNotNullOfOrNull { it.voice }
            ?.let { Foundry.plan(it, gender, profile, quality).parentA }
            ?.takeIf { it !in taken }
        if (preferred != null) return preferred

        return nearestFree(gender, taken)
            ?: preferred
            ?: pool(gender)
            ?: pool(Gender.MALE)
            ?: pool(Gender.FEMALE)
            ?: 0
    }

    /** The first unused speaker of [gender] in pitch order, so the choice is deterministic. */
    private fun nearestFree(gender: Gender, taken: Set<Int>): Int? =
        profile.pool(gender).firstOrNull { it.id !in taken }?.id

    private fun pool(gender: Gender): Int? = profile.pool(gender).firstOrNull()?.id

    /**
     * The speaker closest to the median of the whole pool, that no character already has.
     *
     * Deliberately not "the first male speaker": the narrator reads every line nobody
     * claimed, which is exactly where a listener most needs to tell voices apart, so it
     * takes a voice of its own rather than one the cast is already using.
     */
    private fun narratorSpeaker(taken: Set<Int>): Int {
        val all = Gender.entries.flatMap { profile.pool(it) }
        if (all.isEmpty()) return 0
        val median = all.map { it.f0 }.sorted()[all.size / 2]
        val byPitch = all.sortedBy { abs(it.f0 - median) }
        return (byPitch.firstOrNull { it.id !in taken } ?: byPitch.first()).id
    }

    companion object {
        /** The key [assign] uses for narration; not a character id, and never a manifest name. */
        const val NARRATOR = "\u0000narrator"
    }
}
