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
    /** Delegates to `core:attribution`, which owns segmentation since QUI-008. */
    fun segment(unit: ParagraphUnit): List<Segment> =
        quire.attribution.Segmenter.segment(unit.locator, unit.text)

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
