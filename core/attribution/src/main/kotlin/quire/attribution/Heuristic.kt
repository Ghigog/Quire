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
        tagName(segment.after)?.let { return result(segment, it, DIRECT_TAG, Tier.HEURISTIC, "trailing tag") }
        tagName(segment.before)?.let { return result(segment, it, DIRECT_TAG, Tier.HEURISTIC, "leading tag") }

        if (pronouns) pronounSpeaker(segment.after)?.let {
            return result(segment, it, PRONOUN_TAG, Tier.HEURISTIC, "pronoun tag, one candidate")
        }
        if (pronouns) pronounSpeaker(segment.before)?.let {
            return result(segment, it, PRONOUN_TAG, Tier.HEURISTIC, "pronoun tag, one candidate")
        }

        actionBeatName(segment.before)?.let {
            return result(segment, it, ACTION_BEAT, Tier.HEURISTIC, "action beat")
        }
        return result(segment, null, 0.0, Tier.NARRATOR, "no tag")
    }

    /**
     * A name adjacent to a speech verb: `said Sarah`, `Sarah said`, `Sarah replied quietly`.
     *
     * Requires the verb, not merely a name in the neighbourhood — `Sarah watched him.
     * "Go on."` is an action beat and much weaker, and conflating the two is how a
     * heuristic gets a reputation for confident nonsense.
     */
    private fun tagName(context: String): String? {
        val window = context.take(TAG_WINDOW).lowercase()
        if (SPEECH_VERBS.none { window.contains(it) }) return null
        return byName.firstOrNull { (name, _) -> window.contains(name) }?.second
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
    private fun pronounSpeaker(context: String): String? {
        val window = context.take(TAG_WINDOW).lowercase()
        if (SPEECH_VERBS.none { window.contains(it) }) return null
        val gender = when {
            PRONOUNS_FEMALE.any { window.containsWord(it) } -> Gender.FEMALE
            PRONOUNS_MALE.any { window.containsWord(it) } -> Gender.MALE
            else -> return null
        }
        return byGender[gender]?.singleOrNull()
    }

    /** A manifest name in a nearby sentence with no speech verb: weaker, and scored so. */
    private fun actionBeatName(context: String): String? {
        val window = context.takeLast(BEAT_WINDOW).lowercase()
        return byName.firstOrNull { (name, _) -> window.contains(name) }?.second
    }

    private fun result(segment: Segment, speaker: String?, confidence: Double, tier: Tier, why: String) =
        AttributionResult(
            locator = segment.locator, text = segment.text, kind = segment.kind,
            speakerId = speaker, confidence = if (speaker == null) 0.0 else confidence,
            tier = if (speaker == null && segment.kind == Kind.DIALOGUE) Tier.NARRATOR else tier,
            evidence = why,
        )

    /** Whole-word containment, so "he" does not match inside "the". */
    private fun String.containsWord(word: String): Boolean {
        var from = 0
        while (true) {
            val at = indexOf(word, from)
            if (at < 0) return false
            val beforeOk = at == 0 || !this[at - 1].isLetterOrDigit()
            val afterOk = at + word.length >= length || !this[at + word.length].isLetterOrDigit()
            if (beforeOk && afterOk) return true
            from = at + 1
        }
    }

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

        /** How far either side of a quotation a speech tag can sit, in characters. */
        private const val TAG_WINDOW = 60
        private const val BEAT_WINDOW = 120

        private val SPEECH_VERBS = listOf(
            "said", "says", "asked", "asks", "replied", "replies", "answered", "answers",
            "cried", "shouted", "whispered", "murmured", "muttered", "added", "began",
            "continued", "went on", "observed", "remarked", "insisted", "admitted",
            "called", "returned", "put in", "demanded", "protested", "agreed",
        )
        private val PRONOUNS_FEMALE = listOf("she", "her")
        private val PRONOUNS_MALE = listOf("he", "him", "his")
    }
}
