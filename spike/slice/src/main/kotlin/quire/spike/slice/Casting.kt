package quire.spike.slice

import quire.model.characters.Gender

/**
 * Assigns each character a voice from the engine's speaker range.
 *
 * QUI-011 in spike form. Two rules, in this order:
 *
 * **Sound like the right person.** A character's [Gender] from the manifest selects the
 * pool. Before this existed the cast was arbitrary and audibly wrong on device — the
 * narrator a woman, Sarah a man, Thomas a woman — because ids carry no meaning.
 *
 * **Then sound like different people.** Within a pool, voices are spread across the pitch
 * range rather than taken in order. Adjacent ids in `libritts_r` are neighbouring readers
 * from one corpus and sound alike; the first device test reported no multi-speaker at all
 * purely because it picked 0 and 1.
 *
 * With no profile, or for a character whose gender is unknown, it falls back to spreading
 * across the raw id range — distinct voices of arbitrary sex, which is the old behaviour and
 * still better than collisions.
 */
class Casting(
    private val speakers: Map<String, Gender>,
    private val voiceCount: Int,
    private val profile: VoiceProfile? = null,
    narratorGender: Gender = Gender.NEUTRAL,
) {
    /** Voice for narration. Taken from its own pool so it contrasts with the cast. */
    val narrator: Int = profile
        ?.pool(narratorGender.takeIf { it != Gender.NEUTRAL } ?: Gender.FEMALE)
        ?.let { pool -> pool.getOrNull(pool.size / 2) }
        ?: 0

    private val bySpeaker: Map<String, Int> = run {
        val cast = speakers.keys.sorted()
        val counts = mutableMapOf<Gender, Int>()
        val totals = cast.groupingBy { speakers[it] ?: Gender.UNKNOWN }.eachCount()

        cast.associateWith { id ->
            val gender = speakers[id] ?: Gender.UNKNOWN
            val pool = profile?.pool(gender).orEmpty()
            val nth = counts.getOrDefault(gender, 0).also { counts[gender] = it + 1 }
            val of = totals.getValue(gender)

            if (pool.isEmpty()) spreadOverIds(nth, of) else spreadOver(pool, nth, of, gender)
        }
    }

    /**
     * The `nth` of `of` characters sharing a pool, placed across it.
     *
     * The extremes are avoided: the very lowest and highest pitches in a pool are the
     * caricatures, and a narrator has to be listened to for hours. Two characters land a
     * third and two thirds of the way in, which is far enough apart to tell without either
     * being a growl or a squeak.
     */
    private fun spreadOver(pool: List<Int>, nth: Int, of: Int, gender: Gender): Int {
        val usable = pool.size
        if (usable == 1) return pool.first()
        val fraction = (nth + 1).toDouble() / (of + 1)
        var index = (fraction * (usable - 1)).toInt().coerceIn(0, usable - 1)
        // Never hand a character the narrator's own voice: same sex, same book, and the
        // listener loses the one distinction that has to survive.
        if (pool[index] == narrator && usable > 1) index = (index + 1) % usable
        return pool[index]
    }

    /** No profile: spread across raw ids, away from voice 0. */
    private fun spreadOverIds(nth: Int, of: Int): Int {
        if (voiceCount <= 1) return 0
        val low = voiceCount / 2
        val span = voiceCount - 1 - low
        val step = if (of <= 1) 0 else span * nth / (of - 1)
        return (low + step).coerceIn(0, voiceCount - 1)
    }

    /** The voice for [speakerId], or the narrator's for null and for anyone uncast. */
    fun voiceFor(speakerId: String?): Int = speakerId?.let { bySpeaker[it] } ?: narrator

    val cast: Map<String, Int> get() = bySpeaker

    companion object {
        /**
         * For callers with no manifest: every gender unknown, no profile, narrator 0.
         * The pre-QUI-011 behaviour, kept because an unindexed or unscanned book still has
         * to be read aloud somehow.
         */
        fun untyped(speakers: Collection<String>, voiceCount: Int) =
            Casting(speakers.associateWith { Gender.UNKNOWN }, voiceCount, profile = null)
    }
}
