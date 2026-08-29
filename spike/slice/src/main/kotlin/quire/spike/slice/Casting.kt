package quire.spike.slice

/**
 * Assigns each character a voice from the engine's speaker range.
 *
 * QUI-011 in spike form: no manifest, no user overrides, no per-character metadata — just
 * a deterministic mapping from the speaker ids an index happens to contain onto the 904
 * voices `libritts_r` ships (ADR-0002).
 *
 * **Spread matters more than choice.** Adjacent ids in `libritts_r` are neighbouring
 * readers from one corpus and sound alike; the first device test reported "no
 * multi-speaker" purely because it picked 0 and 1. So voices are spaced as far apart as
 * the cast allows rather than taken in order.
 *
 * Stable for a given cast: the same book always casts the same way, so a reader who puts
 * the device down does not come back to different voices. It is *not* stable across a
 * change of cast — adding a character reshuffles everyone — which is fine while nothing
 * persists a casting, and becomes QUI-011's problem when something does.
 */
class Casting(
    speakers: Collection<String>,
    private val voiceCount: Int,
    /** The narrator's voice. Characters are kept away from it. */
    val narrator: Int = 0,
) {
    private val bySpeaker: Map<String, Int> = run {
        val cast = speakers.distinct().sorted()
        if (cast.isEmpty() || voiceCount <= 1) {
            cast.associateWith { narrator }
        } else {
            // Spread across the upper part of the range, leaving the narrator's
            // neighbourhood alone: with one character that lands mid-range, which is the
            // furthest it can get from voice 0.
            val low = voiceCount / 2
            val span = voiceCount - 1 - low
            cast.withIndex().associate { (i, speaker) ->
                val step = if (cast.size == 1) 0 else span * i / (cast.size - 1)
                speaker to (low + step).coerceIn(0, voiceCount - 1)
            }
        }
    }

    /** The voice for [speakerId], or the narrator's for null and for anyone uncast. */
    fun voiceFor(speakerId: String?): Int =
        speakerId?.let { bySpeaker[it] } ?: narrator

    val cast: Map<String, Int> get() = bySpeaker
}
