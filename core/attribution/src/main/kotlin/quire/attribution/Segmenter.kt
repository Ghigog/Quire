package quire.attribution

import quire.model.Kind

/** A run of a paragraph that is either narration or one span of quoted speech. */
data class Segment(
    val locator: String,
    val text: String,
    val kind: Kind,
    /** Narration before this segment in the same paragraph — where a leading tag lives. */
    val before: String,
    /** Narration after it — where a trailing tag lives. */
    val after: String,
)

/**
 * Splits a paragraph into narration and quoted speech.
 *
 * Both quote conventions are handled, and em-dash dialogue, because a reader's library is
 * not typographically consistent and a book we cannot segment is a book read entirely in
 * the narrator's voice.
 */
object Segmenter {

    private val PAIRS = listOf('"' to '"', '“' to '”', '«' to '»')

    /** Em-dash dialogue, as French and Spanish typesetting use: the line *is* the speech. */
    private val EM_DASH_OPENERS = setOf('—', '―')

    fun segment(locator: String, paragraph: String): List<Segment> {
        val text = paragraph.trim()
        if (text.isEmpty()) return emptyList()

        // A paragraph opening with an em dash is speech to its end, with any tag after a
        // second dash. Treated whole rather than by quote scanning, which would find none.
        if (text.firstOrNull() in EM_DASH_OPENERS) {
            val close = text.indexOfAny(EM_DASH_OPENERS.toCharArray(), startIndex = 1)
            val speech = if (close < 0) text else text.substring(0, close)
            val rest = if (close < 0) "" else text.substring(close)
            return buildList {
                add(Segment("$locator#s0", speech.trim(), Kind.DIALOGUE, "", rest))
                if (rest.isNotBlank()) add(Segment("$locator#s1", rest.trim(), Kind.NARRATION, speech, ""))
            }
        }

        val spans = mutableListOf<Triple<Int, Int, Kind>>()
        var narrationStart = 0
        var i = 0
        while (i < text.length) {
            val pair = PAIRS.firstOrNull { it.first == text[i] }
            if (pair == null) { i++; continue }
            // An apostrophe inside a word is not a quote; only a straight double quote can
            // be confused this way, and requiring a non-letter before it is enough.
            if (text[i] == '"' && i > 0 && text[i - 1].isLetterOrDigit()) { i++; continue }

            val close = text.indexOf(pair.second, startIndex = i + 1)
            val end = if (close < 0) text.length else close + 1
            if (narrationStart < i) spans += Triple(narrationStart, i, Kind.NARRATION)
            spans += Triple(i, end, Kind.DIALOGUE)
            narrationStart = end
            i = end
        }
        if (narrationStart < text.length) spans += Triple(narrationStart, text.length, Kind.NARRATION)
        if (spans.isEmpty()) spans += Triple(0, text.length, Kind.NARRATION)

        return spans.mapIndexedNotNull { n, (start, end, kind) ->
            val body = text.substring(start, end)
            if (body.isBlank()) null
            else Segment(
                locator = "$locator#s$n",
                text = body.trim(),
                kind = kind,
                before = text.substring(0, start),
                after = text.substring(end),
            )
        }
    }
}
