package quire.voice.design

import quire.model.characters.AgeBand
import quire.model.characters.Character
import quire.model.characters.Gender
import quire.model.characters.Voice
import quire.model.characters.VoiceSource
import quire.voice.foundry.SpeakerProfile

/**
 * ADR-0006's job C: describe how a character sounds, from a handful of lines the scan is
 * already confident about.
 *
 * **Takes pre-filtered lines, not attribution internals.** The caller (the scan, eventually
 * QUI-007) decides what counts as "confidently attributed" — Tier 1's explicit-tag set, per
 * ADR-0006 — and hands over the text alone. That keeps this module decoupled from
 * `core:attribution`, which the module boundary rule (QUI-001) would not allow it to depend
 * on anyway.
 *
 * **Does not write prose with an SLM.** QUI-006 does not exist yet, and prompting it is out
 * of scope for QUI-037. [description] is a template sentence instead — a complete, usable
 * v1, not a stub — and `source` stays [VoiceSource.AUTO] either way, so a future SLM-backed
 * writer is a drop-in replacement with no schema change.
 */
class VoiceDesigner(private val profile: SpeakerProfile) {

    /**
     * A descriptor for [character], or null if [explicitLines] is too thin to judge from.
     *
     * Matches the handoff's proposed fallback for a character with too few confidently-
     * attributed lines: no descriptor, so casting falls back to plain gender/pitch — better
     * than inventing a voice from evidence that would not support one.
     */
    fun design(character: Character, explicitLines: List<String>): Voice? {
        if (explicitLines.size < MIN_LINES) return null

        return Voice(
            targetF0Hz = centerF0(character.gender) + ageOffsetHz(character.ageBand),
            lengthScale = lengthScaleFor(explicitLines),
            description = describe(character),
            source = VoiceSource.AUTO,
        )
    }

    /** The gender's measured median F0, or an average of both if this character's is unknown. */
    private fun centerF0(gender: Gender): Double {
        profile.medianF0(gender)?.let { return it }
        val known = listOfNotNull(profile.medianF0(Gender.MALE), profile.medianF0(Gender.FEMALE))
        return if (known.isNotEmpty()) known.average() else UNMEASURED_FALLBACK_HZ
    }

    private fun lengthScaleFor(lines: List<String>): Double {
        val avgWords = lines.map { it.trim().split(WHITESPACE).count(String::isNotBlank) }.average()
        return (avgWords / REFERENCE_WORDS_PER_LINE).coerceIn(MIN_LENGTH_SCALE, MAX_LENGTH_SCALE)
    }

    private fun describe(character: Character): String {
        val age = when (character.ageBand) {
            AgeBand.CHILD -> "young"
            AgeBand.TEEN -> "teenage"
            AgeBand.ELDER -> "elderly"
            AgeBand.ADULT, AgeBand.UNKNOWN -> "adult"
        }
        val genderWord = when (character.gender) {
            Gender.MALE -> "man"
            Gender.FEMALE -> "woman"
            Gender.NEUTRAL, Gender.UNKNOWN -> "person"
        }
        val traits = character.traits.take(2)
        val traitClause = if (traits.isEmpty()) "" else ", ${traits.joinToString(", ")}"
        return "$age $genderWord$traitClause."
    }

    companion object {
        /** Matches QUI-007's own walk-on discard threshold: fewer mentions, no descriptor. */
        const val MIN_LINES = 3

        /**
         * How far a character's target pitch shifts from their gender's measured center,
         * by age band. **Assumed, not measured** — no labelled data exists for how age
         * shifts F0 in this cast. Kept apart from the measured gender centers deliberately;
         * see this ticket's Worklog.
         */
        private fun ageOffsetHz(ageBand: AgeBand): Double = when (ageBand) {
            AgeBand.CHILD -> 40.0
            AgeBand.TEEN -> 15.0
            AgeBand.ELDER -> -10.0
            AgeBand.ADULT, AgeBand.UNKNOWN -> 0.0
        }

        /**
         * `voiceprofile.py`'s own measured trough between the male and female F0 modes —
         * reused here only for a character whose gender is unknown and the fixture happens
         * to carry no measurements at all. Never hit against the real fixture.
         */
        private const val UNMEASURED_FALLBACK_HZ = 145.0

        /** Assumed, not measured: a line of this length reads at the model's default pace. */
        private const val REFERENCE_WORDS_PER_LINE = 12.0

        /** A narrow band around the default, so a handful of lines cannot caricature a pace. */
        private const val MIN_LENGTH_SCALE = 0.85
        private const val MAX_LENGTH_SCALE = 1.2

        private val WHITESPACE = Regex("\\s+")
    }
}
