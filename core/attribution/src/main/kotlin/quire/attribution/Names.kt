package quire.attribution

/**
 * Finding people's names in prose, and telling a speech tag from a mention.
 *
 * Ported from the QUI-018 spike rather than rewritten: this logic was scored against PDNC
 * and against the labelled fixtures, and a fresh implementation would be an unmeasured one
 * wearing the measured one's numbers.
 */
internal object Names {

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

    // Immediately after a closing quote: `," said Sarah.` / `," Sarah said.`
    private val AFTER_VERB_NAME = Regex("^[\\s,.;:!?—–-]*\\b($VERBS)\\s+($NAME)")
    private val AFTER_NAME_VERB = Regex("^[\\s,.;:!?—–-]*($NAME)\\s+\\b($VERBS)\\b")
    private val AFTER_PRONOUN =
        Regex("^[\\s,.;:!?—–-]*\\b(he|she)\\b\\s+\\b($VERBS)\\b", RegexOption.IGNORE_CASE)

    // Immediately before an opening quote: `Sarah said, "…"` / `said Sarah, "…"`
    private val BEFORE_NAME_VERB = Regex("($NAME)\\s+\\b($VERBS)\\b[\\s,:—–-]*$")
    private val BEFORE_VERB_NAME = Regex("\\b($VERBS)\\s+($NAME)[\\s,:—–-]*$")
    private val BEFORE_PRONOUN =
        Regex("\\b(he|she)\\b\\s+\\b($VERBS)\\b[\\s,:—–-]*$", RegexOption.IGNORE_CASE)

    private val ANY_NAME = Regex(NAME)

    /** The name in an explicit speech tag around a quotation, if there is one. */
    fun tagName(before: String, after: String): String? {
        AFTER_VERB_NAME.find(after)?.let { return clean(it.groupValues[2]) }
        AFTER_NAME_VERB.find(after)?.let { return clean(it.groupValues[1]) }
        val trimmed = before.trimEnd()
        BEFORE_NAME_VERB.find(trimmed)?.let { return clean(it.groupValues[1]) }
        BEFORE_VERB_NAME.find(trimmed)?.let { return clean(it.groupValues[2]) }
        return null
    }

    /**
     * The pronoun in a speech tag around a quotation — `she said`, `said he`.
     *
     * Requires the verb, so `She crossed to the window. "I know."` is not a tag: that is a
     * person doing something near a quotation, which is an action beat and much weaker.
     */
    fun tagPronoun(before: String, after: String): String? {
        AFTER_PRONOUN.find(after)?.let { return it.groupValues[1].lowercase() }
        BEFORE_PRONOUN.find(before.trimEnd())?.let { return it.groupValues[1].lowercase() }
        return null
    }

    /** Every plausible name in a sentence, in order, skipping addressee position. */
    fun namesIn(sentence: String?): List<String> {
        if (sentence.isNullOrBlank()) return emptyList()
        return ANY_NAME.findAll(sentence).mapNotNull { m ->
            val preceding = sentence.substring(0, m.range.first)
                .trimEnd().takeLastWhile { !it.isWhitespace() }.lowercase().trim(',', '.', ';', ':')
            if (preceding in ADDRESSEE_PREPOSITIONS) null else clean(m.value)
        }.toList()
    }

    /**
     * The same text with every quoted span blanked out.
     *
     * A segment's `before` and `after` are the *whole* rest of the paragraph, so on a line
     * of pure back-and-forth they carry the neighbouring quotations — and the first word of
     * a quotation is capitalised because it begins the speech, not because it is a name.
     * That is what made `"Dammit." "Quite so."` report a character called Dammit.
     */
    fun withoutQuotedText(text: String): String {
        val out = StringBuilder(text)
        var i = 0
        while (i < out.length) {
            val pair = QUOTE_PAIRS.firstOrNull { it.first == out[i] }
            if (pair == null) { i++; continue }
            val close = out.indexOf(pair.second.toString(), i + 1)
            val end = if (close < 0) out.length else close + 1
            for (j in i until end) out[j] = ' '
            i = end
        }
        return out.toString()
    }

    private val QUOTE_PAIRS = listOf('"' to '"', '\u201c' to '\u201d', '\u00ab' to '\u00bb')

    /** Trim a raw regex hit down to a plausible name, or null if it is not one. */
    fun clean(raw: String): String? {
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

    /** Sentence split, simple on purpose: this only feeds name-window heuristics. */
    fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
}
