package quire.tts.casting

import quire.model.characters.AgeBand
import quire.model.characters.Gender

/**
 * Which voice ids a caster may choose from for a given gender and age band.
 *
 * Supplied by the caller, never derived here — same decoupling as
 * [quire.voice.design.VoiceDesigner]: which ids sound like which gender and age is a
 * property of the TTS engine's speaker set (`core:voice`'s `SpeakerProfile`, pitch
 * measurements, a foundry-generated blend), and `core:tts` cannot depend on `core:voice`
 * without reaching sideways across the module boundary (QUI-001). The app layer composes
 * the two; this class only spends what it is handed.
 *
 * Degrades gracefully rather than failing: an unmatched (gender, age) pair falls back to
 * every id of the right gender, then to [fallback] — a near miss beats no voice at all,
 * the same rule `spike/slice/Casting.kt` established.
 */
class VoicePool(
    private val groups: Map<Pair<Gender, AgeBand>, List<Int>>,
    private val fallback: List<Int>,
) {
    fun candidatesFor(gender: Gender, ageBand: AgeBand): List<Int> {
        groups[gender to ageBand]?.takeIf { it.isNotEmpty() }?.let { return it }
        val sameGender = groups.filterKeys { it.first == gender }.values.flatten().distinct()
        if (sameGender.isNotEmpty()) return sameGender
        return fallback
    }

    companion object {
        /** One pool, same candidates for every character — for a caller with no groups to give. */
        fun flat(ids: List<Int>) = VoicePool(emptyMap(), ids)
    }
}
