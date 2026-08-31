package quire.attribution

import quire.model.AttributionResult
import quire.model.Kind
import quire.model.Tier
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender

/**
 * Tier 1: resolve a speaker from the text around a line, with no model.
 *
 * Every line resolved here is a line the SLM never sees, which is what keeps a whole-book
 * scan inside the time budget (`docs/architecture.md` §5). It resolves a third of dialogue
 * on realistic prose, so it narrows the model's job rather than replacing it.
 *
 * **It declines rather than guesses.** A name not in the manifest yields no attribution and
 * never invents a character; an ambiguous pronoun yields none either. A wrong speaker is
 * heard immediately and a missing one is merely flat, so the asymmetry is deliberate.
 */
class Heuristic(
    private val manifest: CharacterManifest,
    /**
     * Whether to resolve `she said` against a cast holding one woman.
     *
     * Switchable so the rule can be scored against its own absence on identical text,
     * which is the only fair way to say what it bought — the same reason QUI-028 kept a
     * flag for the action-beat rule.
     */
    private val pronouns: Boolean = true,
    /**
     * Whether a name in the sentence before a quotation may attribute it.
     *
     * Switchable because QUI-028 measured that it trades precision for accuracy — it fires
     * on quotations carrying no tag, which is exactly where it has no evidence.
     */
    private val actionBeats: Boolean = true,
) {

    /** Manifest names and aliases, folded, longest first so "Mr Ashcombe" beats "Ashcombe". */
    private val byName: List<Pair<String, String>> =
        manifest.characters
            .flatMap { character ->
                (listOf(character.displayName) + character.aliases).map { it.lowercase() to character.id }
            }
            .sortedByDescending { it.first.length }

    /** Characters of each gender, for resolving `she said` when only one woman can be meant. */
    private val byGender: Map<Gender, List<String>> =
        manifest.characters.groupBy({ it.gender }, { it.id })

    fun attribute(locator: String, paragraph: String): List<AttributionResult> =
        Segmenter.segment(locator, paragraph).map(::resolve)

    fun attributeAll(paragraphs: List<Pair<String, String>>): List<AttributionResult> =
        paragraphs.flatMap { (locator, text) -> attribute(locator, text) }

    private fun resolve(segment: Segment): AttributionResult {
        if (segment.kind == Kind.NARRATION) {
            return result(segment, null, 0.0, Tier.NARRATOR, "narration")
        }
        // Tags nearest the speech first: a trailing `," said Sarah` and a leading
        // `Sarah said, "` both sit adjacent to it, while an action beat is a weaker signal
        // further away and is tried last.
        Names.tagName(segment.before, segment.after)?.let { name ->
            known(name)?.let { return result(segment, it, DIRECT_TAG, Tier.HEURISTIC, "speech tag") }
            // A tag naming somebody the cast has never heard of. Recording the name is the
            // difference between a transcript that explains a miss and one that shrugs —
            // it is usually a minor character the scan missed, not a parsing failure.
            return result(segment, null, 0.0, Tier.NONE, "tag names $name, not in cast")
        }

        // A pronoun tag we cannot pin down is not the same as no tag at all: the line is
        // far more tractable for the model, which is what QUI-009 will choose targets by.
        val pronoun = Names.tagPronoun(segment.before, segment.after)
        if (pronoun != null) {
            if (pronouns) {
                pronounSpeaker(segment.before, segment.after)?.let {
                    return result(segment, it, PRONOUN_TAG, Tier.HEURISTIC, "pronoun tag, one candidate")
                }
            }
            return result(segment, null, 0.0, Tier.NONE, "pronoun speech tag")
        }

        if (actionBeats) {
            actionBeatName(segment.before)?.let {
                return result(segment, it, ACTION_BEAT, Tier.HEURISTIC, "action beat")
            }
        }
        return result(segment, null, 0.0, Tier.NONE, "no tag")
    }

    /** A manifest character for this name or alias, or null: never a new character. */
    private fun known(name: String): String? {
        val folded = name.lowercase()
        return byName.firstOrNull { (candidate, _) -> candidate == folded }?.second
            ?: byName.firstOrNull { (candidate, _) -> folded.endsWith(candidate) }?.second
    }

    /**
     * `she said` where the cast holds exactly one woman.
     *
     * This is the cheapest coverage left in the project: measured on the slice's own
     * chapter, three of six Tier 1 misses carry a pronoun tag and nothing else. It works
     * only where the answer is forced — one candidate of that gender — because with two
     * women in the room a pronoun narrows the field without choosing, and choosing anyway
     * would be a guess wearing a confidence score. Those go to QUI-009.
     */
    private fun pronounSpeaker(before: String, after: String): String? {
        val gender = when (Names.tagPronoun(before, after)) {
            "she" -> Gender.FEMALE
            "he" -> Gender.MALE
            else -> return null
        }
        return byGender[gender]?.singleOrNull()
    }

    /** A manifest name in the sentence before the quote, with no speech verb: weaker. */
    private fun actionBeatName(before: String): String? =
        Names.sentences(before).lastOrNull()
            ?.let(Names::namesIn)
            ?.firstNotNullOfOrNull(::known)

    private fun result(segment: Segment, speaker: String?, confidence: Double, tier: Tier, why: String) =
        AttributionResult(
            locator = segment.locator, text = segment.text, kind = segment.kind,
            speakerId = speaker, confidence = if (speaker == null) 0.0 else confidence,
            tier = tier,
            evidence = why,
        )

    companion object {
        /**
         * Declared confidences, from QUI-008's requirements.
         *
         * **Known to be optimistic.** QUI-028 measured explicit tags at 68.6% precision on
         * PDNC against the 0.95 claimed here. They are left as specified rather than
         * quietly adjusted, because moving them without a measurement would swap one
         * fiction for another; recalibrating against PDNC is QUI-009's prerequisite.
         */
        const val DIRECT_TAG = 0.95
        const val ACTION_BEAT = 0.75

        /**
         * A pronoun tag with exactly one candidate. Below a direct tag because it inherits
         * the manifest's gender, which a scan can get wrong, and above an action beat
         * because the speech verb makes it speech rather than proximity.
         */
        const val PRONOUN_TAG = 0.85

    }
}
