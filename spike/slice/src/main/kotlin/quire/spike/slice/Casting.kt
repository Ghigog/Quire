package quire.spike.slice

import quire.model.characters.Gender
import quire.model.characters.Voice
import quire.voice.foundry.BlendPlan
import quire.voice.foundry.Foundry
import quire.voice.foundry.QualityList
import quire.voice.foundry.SpeakerProfile

/**
 * Assigns each character a voice from the engine's speaker range.
 *
 * QUI-011 in spike form, and since QUI-037 a resolver rather than pure pool selection
 * (ADR-0009's consequence for this class). Two paths, tried in this order per character:
 *
 * **A voice descriptor resolves to a generated blend.** If [descriptors] carries a
 * character's [Voice] with a `targetF0Hz`, `core:voice`'s [Foundry] picks the two real
 * speakers either side of it — skipping any [quality] has flagged poor — and the blend
 * fraction between them. [voiceFor] still returns a single [Int] for callers that only need
 * one (today's audio pipeline does); [blendFor] exposes the full plan for a caller that can
 * actually interpolate two embeddings, which nothing in this repo can yet (QUI-010 is Todo).
 *
 * **Otherwise, the original two rules still apply**, unchanged, for a character with no
 * descriptor — an unscanned book, or one job C declined to describe (QUI-037's
 * `VoiceDesigner`, for want of enough confidently-attributed lines):
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
    private val descriptors: Map<String, Voice> = emptyMap(),
    private val quality: QualityList = QualityList.EMPTY,
) {
    /** [profile] adapted to the shape `core:voice`'s [Foundry] consumes. Never depended on
     *  the other way around — spike code is never a dependency of `core/` (CLAUDE.md §3). */
    private val speakerProfile: SpeakerProfile? = profile?.let { vp ->
        SpeakerProfile(
            Gender.entries.flatMap { gender ->
                vp.pool(gender).mapNotNull { id -> vp.f0Of(id)?.let { f0 -> SpeakerProfile.Voice(id, f0, gender) } }
            },
        )
    }

    /** A blend plan per character carrying a resolvable descriptor. Empty without a profile. */
    private val blendPlans: Map<String, BlendPlan> = speakerProfile?.let { sp ->
        descriptors.filterValues { it.targetF0Hz != null }
            .mapValues { (id, voice) -> Foundry.plan(voice, speakers[id] ?: Gender.UNKNOWN, sp, quality) }
    }.orEmpty()
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
            val pool = candidatesFor(gender)
            val nth = counts.getOrDefault(gender, 0).also { counts[gender] = it + 1 }
            val of = totals.getValue(gender)

            if (pool.isEmpty()) spreadOverIds(nth, of) else spreadOver(pool, nth, of)
        }
    }

    /**
     * A gender's voices with the narrator's neighbourhood removed.
     *
     * Excluding the narrator's *id* is not enough, and assuming otherwise shipped a cast
     * where the narrator and Sarah were different speakers at an identical 188.5 Hz. Two
     * voices of the same sex and the same pitch are the same voice to a listener, and the
     * narrator-versus-character distinction is the one the product cannot afford to lose.
     * So a band either side of the narrator's pitch is off limits — unless that would empty
     * the pool, in which case a near miss beats no voice.
     */
    private fun candidatesFor(gender: Gender): List<Int> {
        val pool = profile?.pool(gender).orEmpty()
        val narratorF0 = profile?.f0Of(narrator) ?: return pool
        val far = pool.filter { id ->
            val f0 = profile.f0Of(id) ?: return@filter false
            kotlin.math.abs(f0 - narratorF0) >= GUARD_HZ
        }
        return far.ifEmpty { pool }
    }

    /**
     * The `nth` of `of` characters sharing a pool, placed across it.
     *
     * The extremes are avoided: the very lowest and highest pitches in a pool are the
     * caricatures, and a narrator has to be listened to for hours. Two characters land a
     * third and two thirds of the way in, which is far enough apart to tell without either
     * being a growl or a squeak.
     */
    private fun spreadOver(pool: List<Int>, nth: Int, of: Int): Int {
        val usable = pool.size
        if (usable == 1) return pool.first()
        val fraction = (nth + 1).toDouble() / (of + 1)
        return pool[(fraction * (usable - 1)).toInt().coerceIn(0, usable - 1)]
    }

    /** No profile: spread across raw ids, away from voice 0. */
    private fun spreadOverIds(nth: Int, of: Int): Int {
        if (voiceCount <= 1) return 0
        val low = voiceCount / 2
        val span = voiceCount - 1 - low
        val step = if (of <= 1) 0 else span * nth / (of - 1)
        return (low + step).coerceIn(0, voiceCount - 1)
    }

    /**
     * The voice for [speakerId], or the narrator's for null and for anyone uncast.
     *
     * A resolved blend collapses to its first parent — a real, distinct speaker, just not
     * yet the generated one — since nothing in this repo can interpolate two embeddings at
     * runtime (QUI-010 is Todo). [blendFor] carries the full plan for when something can.
     */
    fun voiceFor(speakerId: String?): Int =
        speakerId?.let { blendPlans[it]?.parentA ?: bySpeaker[it] } ?: narrator

    /** The full realisation plan for [speakerId], or null if it has no resolvable descriptor. */
    fun blendFor(speakerId: String?): BlendPlan? = speakerId?.let { blendPlans[it] }

    val cast: Map<String, Int> get() = bySpeaker

    companion object {
        /**
         * How far a character's pitch must sit from the narrator's, in Hz.
         *
         * 25 Hz is roughly a fifth of each pool's spread, wide enough to hear across a
         * chapter and narrow enough to leave hundreds of voices in play.
         */
        const val GUARD_HZ = 25.0

        /**
         * For callers with no manifest: every gender unknown, no profile, narrator 0.
         * The pre-QUI-011 behaviour, kept because an unindexed or unscanned book still has
         * to be read aloud somehow.
         */
        fun untyped(speakers: Collection<String>, voiceCount: Int) =
            Casting(speakers.associateWith { Gender.UNKNOWN }, voiceCount, profile = null)
    }
}
