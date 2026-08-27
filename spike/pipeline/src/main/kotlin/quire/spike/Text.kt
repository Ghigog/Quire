package quire.spike

/** Quote handling and sentence splitting. No model, no I/O. */
object Text {

    /** Straight and typographic quote pairs we recognise. */
    private val QUOTE_PAIRS = listOf('"' to '"', '“' to '”')

    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "st", "sr", "jr", "capt", "col", "gen",
        "lt", "sgt", "rev", "hon", "esq", "vs", "etc", "e.g", "i.e",
    )

    /**
     * Split a paragraph into narration and quoted-speech segments, in order.
     *
     * An unclosed quote is treated as running to the end of the paragraph: that is the
     * convention for multi-paragraph speech, where the opening quote is repeated and the
     * closing one omitted until the speech ends.
     */
    fun segment(unit: ParagraphUnit): List<Segment> {
        val text = unit.text
        val spans = mutableListOf<Triple<Int, Int, Kind>>() // start, end, kind
        var i = 0
        var narrationStart = 0
        while (i < text.length) {
            val open = QUOTE_PAIRS.firstOrNull { text[i] == it.first }
            if (open == null) { i++; continue }
            // A closing curly quote never opens; a straight quote is ambiguous, so an
            // apostrophe inside a word is excluded by requiring a non-letter before it.
            if (i > 0 && text[i - 1].isLetterOrDigit()) { i++; continue }
            val close = text.indexOf(open.second, startIndex = i + 1)
            val end = if (close == -1) text.length else close + 1
            if (narrationStart < i) spans += Triple(narrationStart, i, Kind.NARRATION)
            spans += Triple(i, end, Kind.DIALOGUE)
            narrationStart = end
            i = end
        }
        if (narrationStart < text.length) spans += Triple(narrationStart, text.length, Kind.NARRATION)
        if (spans.isEmpty()) spans += Triple(0, text.length, Kind.NARRATION)

        return spans.mapIndexedNotNull { n, (start, end, kind) ->
            val body = text.substring(start, end)
            if (body.isBlank()) return@mapIndexedNotNull null
            Segment(
                locator = "${unit.locator}#s$n",
                text = body.trim(),
                kind = kind,
                before = text.substring(0, start),
                after = text.substring(end),
            )
        }
    }

    /** Strip surrounding quote marks from a dialogue segment. */
    fun unquote(s: String): String =
        s.trim().trim('"', '“', '”').trim()

    /**
     * Split into sentences. Deliberately simple: it handles the abbreviation cases that
     * actually appear in speech tags (titles) and nothing more clever.
     */
    fun sentences(text: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            sb.append(c)
            if (c == '.' || c == '!' || c == '?') {
                val word = sb.toString().trimEnd('.', '!', '?')
                    .takeLastWhile { !it.isWhitespace() }.lowercase()
                val isAbbrev = c == '.' && word in ABBREVIATIONS
                val next = text.getOrNull(i + 1)
                if (!isAbbrev && (next == null || next.isWhitespace())) {
                    out += sb.toString().trim()
                    sb.clear()
                }
            }
            i++
        }
        if (sb.isNotBlank()) out += sb.toString().trim()
        return out.filter { it.isNotBlank() }
    }
}
