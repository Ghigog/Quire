package quire.spike

/**
 * Tier 1 heuristic attribution (QUI-008).
 *
 * On the reference device every line resolved here is a line the SLM never has to see,
 * so coverage is a performance feature and not just an accuracy one — see
 * docs/device-profile.md §2.
 */
object Tier1 {

    /**
     * Whether to attribute a quote from a name in an adjacent sentence.
     *
     * Switchable because QUI-028 measures whether the rule pays for itself. It fires on
     * quotations that carry no tag at all, which is precisely where it has no evidence.
     */
    var useActionBeats = true

    private const val TITLES =
        "Mr|Mrs|Ms|Miss|Dr|Prof|St|Sir|Lady|Lord|Captain|Capt|Colonel|Col|Major|Aunt|Uncle|Father|Mother"

    private const val VERBS =
        "said|says|say|asked|asks|replied|replies|answered|answers|cried|cries|shouted|" +
        "whispered|whispers|murmured|muttered|mutters|exclaimed|continued|continues|added|adds|" +
        "began|observed|remarked|returned|retorted|protested|insisted|called|calls|screamed|" +
        "sighed|laughed|groaned|breathed|drawled|snapped|snaps|demanded|demands|inquired|" +
        "repeated|repeats|admitted|agreed|offered|echoed|mused|ventured|urged|stammered|" +
        "faltered|announced|declared|growled|hissed|barked|put in|went on|broke in|interrupted"

    /** A name that follows one of these is being spoken *to*, not speaking. */
    private val ADDRESSEE_PREPOSITIONS = setOf(
        "to", "at", "toward", "towards", "for", "with", "about", "of", "from",
        "by", "near", "beside", "against", "behind", "beyond", "upon",
    )

    /** Capitalised words that start sentences rather than name people. */
    private val NOT_NAMES = setOf(
        "The", "A", "An", "And", "But", "Or", "So", "Then", "When", "While", "After",
        "Before", "There", "That", "This", "These", "Those", "What", "Why", "How",
        "He", "She", "They", "It", "We", "You", "I", "His", "Her", "Their", "Its",
        "Yes", "No", "Oh", "Ah", "Well", "Now", "Here", "If", "As", "At", "In", "On",
        "Of", "For", "To", "With", "From", "By", "Not", "Never", "Only", "Even",
    )

    private const val NAME = "(?:(?:$TITLES)\\.?\\s+)?[A-Z][a-zA-Z'’-]+(?:\\s+[A-Z][a-zA-Z'’-]+)?"

    // Text immediately after a closing quote: `," said Sarah.` / `," Sarah said.`
    private val AFTER_VERB_NAME = Regex("^[\\s,.;:!?—–-]*\\b($VERBS)\\s+($NAME)")
    private val AFTER_NAME_VERB = Regex("^[\\s,.;:!?—–-]*($NAME)\\s+\\b($VERBS)\\b")
    private val AFTER_PRONOUN = Regex("^[\\s,.;:!?—–-]*\\b(he|she|they|I)\\s+\\b($VERBS)\\b", RegexOption.IGNORE_CASE)

    // Text immediately before an opening quote: `Sarah said, "…"` / `said Sarah, "…"`
    private val BEFORE_NAME_VERB = Regex("($NAME)\\s+\\b($VERBS)\\b[\\s,:—–-]*$")
    private val BEFORE_VERB_NAME = Regex("\\b($VERBS)\\s+($NAME)[\\s,:—–-]*$")

    private val ANY_NAME = Regex(NAME)

    /** A name must sit beside this many quotes before adjacency alone admits it. */
    const val ADJACENCY_MIN = 2

    /** The model-free roster, and where each name came from. */
    data class Roster(
        val fromTags: Map<String, Int>,
        val fromAdjacency: Map<String, Int>,
    ) {
        val names: Set<String> =
            fromTags.keys + fromAdjacency.filterValues { it >= ADJACENCY_MIN }.keys
    }

    /**
     * Pass A: build a roster with no model at all.
     *
     * Two sources. A name in explicit speech-tag position ("said X") is a speaking
     * character by construction, so one sighting is enough. A name merely standing next
     * to a quote is weaker evidence — it could be the person being addressed, or someone
     * who happens to be in the room — so it takes [ADJACENCY_MIN] sightings.
     *
     * The second source exists because modern prose can run whole scenes on action beats
     * without a single speech tag: measured on fixtures/attribution/beats.tsv, tags alone
     * found one of the two speakers and left every one of the other's lines unattributable.
     */
    fun bootstrapRoster(paragraphs: List<ParagraphUnit>): Roster {
        val tags = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, Int>()
        for (p in paragraphs) {
            for (seg in Text.segment(p)) {
                if (seg.kind != Kind.DIALOGUE) continue
                val tagged = explicitTagName(seg)
                if (tagged != null) {
                    tags.merge(tagged, 1, Int::plus)
                } else {
                    for (name in adjacencyCandidates(seg)) adjacency.merge(name, 1, Int::plus)
                }
            }
        }
        return Roster(tags, adjacency - tags.keys)
    }

    /** Names in the sentences either side of a quote, excluding addressees. */
    private fun adjacencyCandidates(seg: Segment): List<String> =
        listOfNotNull(Text.sentences(seg.before).lastOrNull(), Text.sentences(seg.after).firstOrNull())
            .flatMap { namesIn(it) }

    /** The name in an explicit speech tag around this segment, if there is one. */
    private fun explicitTagName(seg: Segment): String? {
        val after = seg.after
        AFTER_VERB_NAME.find(after)?.let { return clean(it.groupValues[2]) }
        AFTER_NAME_VERB.find(after)?.let { return clean(it.groupValues[1]) }
        val before = seg.before.trimEnd()
        BEFORE_NAME_VERB.find(before)?.let { return clean(it.groupValues[1]) }
        BEFORE_VERB_NAME.find(before)?.let { return clean(it.groupValues[2]) }
        return null
    }

    /**
     * Pass B: attribute every segment of every paragraph.
     *
     * [roster] gates which names may be assigned. An unrecognised name yields no
     * attribution rather than inventing a character (QUI-008).
     */
    fun attribute(paragraphs: List<ParagraphUnit>, roster: Set<String>): List<AttributionResult> =
        paragraphs.flatMap { p -> Text.segment(p).map { attributeSegment(it, roster) } }

    private fun attributeSegment(seg: Segment, roster: Set<String>): AttributionResult {
        if (seg.kind == Kind.NARRATION) {
            return result(seg, null, 1.0, Tier.HEURISTIC, "narration")
        }

        explicitTagName(seg)?.let { name ->
            return if (name in roster) {
                result(seg, name, Thresholds.EXPLICIT_TAG, Tier.HEURISTIC, "explicit speech tag")
            } else {
                // A tag we parsed but cannot vouch for: never invent a character.
                result(seg, null, 0.0, Tier.NONE, "tag names unknown '$name'")
            }
        }

        if (AFTER_PRONOUN.containsMatchIn(seg.after)) {
            // Strong signal that this is dialogue with a real speaker, and an equally
            // strong signal Tier 1 cannot name them. Handing Tier 2 the reason is worth
            // more than a guess here.
            return result(seg, null, 0.0, Tier.NONE, "pronoun speech tag")
        }

        if (!useActionBeats) return result(seg, null, 0.0, Tier.NONE, "unattributed")

        actionBeatName(Text.sentences(seg.before).lastOrNull(), roster)?.let {
            return result(seg, it, Thresholds.ACTION_BEAT, Tier.HEURISTIC, "action beat before")
        }
        actionBeatName(Text.sentences(seg.after).firstOrNull(), roster)?.let {
            return result(seg, it, Thresholds.ACTION_BEAT, Tier.HEURISTIC, "action beat after")
        }

        return result(seg, null, 0.0, Tier.NONE, "unattributed")
    }

    /**
     * A roster name in an adjacent sentence, taken as the speaker of a neighbouring
     * quote. Names in addressee position are skipped: in `she said to Sarah`, Sarah is
     * being spoken to.
     */
    private fun actionBeatName(sentence: String?, roster: Set<String>): String? =
        namesIn(sentence).firstOrNull { it in roster }

    /** Every plausible name in a sentence, in order, skipping addressee position. */
    private fun namesIn(sentence: String?): List<String> {
        if (sentence.isNullOrBlank()) return emptyList()
        return ANY_NAME.findAll(sentence).mapNotNull { m ->
            val preceding = sentence.substring(0, m.range.first)
                .trimEnd().takeLastWhile { !it.isWhitespace() }.lowercase().trim(',', '.', ';', ':')
            if (preceding in ADDRESSEE_PREPOSITIONS) null else clean(m.value)
        }.toList()
    }

    /** Trim a raw regex hit down to a plausible name, or null if it is not one. */
    private fun clean(raw: String): String? {
        var words = raw.trim().split(Regex("\\s+"))
        while (words.size > 1 && words.last() in NOT_NAMES) words = words.dropLast(1)
        if (words.isEmpty()) return null
        val name = words.joinToString(" ")
        val head = words.first().trimEnd('.')
        val isTitled = Regex("^(?:$TITLES)$").matches(head)
        if (!isTitled && words.size == 1 && name in NOT_NAMES) return null
        if (isTitled && words.size == 1) return null // a bare "Mr" names nobody
        return name
    }

    private fun result(seg: Segment, speaker: String?, confidence: Double, tier: Tier, why: String) =
        AttributionResult(
            locator = seg.locator,
            text = if (seg.kind == Kind.DIALOGUE) Text.unquote(seg.text) else seg.text,
            kind = seg.kind,
            speakerId = speaker,
            confidence = confidence,
            tier = tier,
            evidence = why,
        )
}
