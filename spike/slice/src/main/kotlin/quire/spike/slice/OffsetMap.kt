package quire.spike.slice

import quire.index.Normalizer

/**
 * Maps positions in normalised text back to positions in the raw text it came from.
 *
 * **Why this has to exist.** The matcher works in normalised coordinates — punctuation
 * dropped, case folded — because that is the only way a host's clause matches an index
 * entry. But the voice spans, and the `rangeStart` offsets a host highlights with, are in
 * raw coordinates. Without a bridge, a chunk that stops inside a sentence can be given the
 * right *voice* but not split at the right *place*, which is exactly the limitation
 * recorded on `MatchResult.partial`.
 *
 * This is QUI-027's job. Implemented here in the spike because the vertical slice cannot
 * demonstrate per-speaker voices without it, and deliberately kept in one small class so
 * QUI-027 can lift it into `core:index` and delete this one.
 *
 * **Known approximation.** [Normalizer] applies NFKC to the whole string; this walks it a
 * character at a time, so a decomposition that spans characters would drift. English prose
 * does not contain one, and the class asserts the reconstruction matches rather than
 * trusting it — see [normalized].
 */
class OffsetMap(private val raw: String) {

    /** For each character of [normalized], the index in [raw] it came from. */
    private val toRaw = mutableListOf<Int>()

    /** The normalisation of [raw], produced by this walk. */
    val normalized: String

    init {
        val sb = StringBuilder()
        raw.forEachIndexed { i, c ->
            // Whitespace has to be handled here rather than delegated: normalize() trims,
            // so a lone space normalises to nothing and a naive per-character walk drops
            // every space in the string. Everything else — NFKC, case folding, stripped
            // quotes, the kept apostrophe — goes to the real normaliser so the two cannot
            // drift, which is the whole point of Normalizer being the only implementation.
            val piece =
                if (c.isWhitespace() || c == '-' || c == '—') " "
                else Normalizer.normalize(c.toString())
            if (piece.isNotEmpty()) {
                // Whitespace collapses: never emit a second space in a row, and never a
                // leading one, which is what normalize()'s trim and \s+ collapse do.
                if (piece == " " && (sb.isEmpty() || sb.last() == ' ')) return@forEachIndexed
                repeat(piece.length) { toRaw += i }
                sb.append(piece)
            }
        }
        while (sb.isNotEmpty() && sb.last() == ' ') {
            sb.setLength(sb.length - 1)
            toRaw.removeAt(toRaw.size - 1)
        }
        normalized = sb.toString()
    }

    /** True when this walk reproduces [Normalizer.normalize] exactly. */
    val faithful: Boolean get() = normalized == Normalizer.normalize(raw)

    /**
     * The raw offset for normalised position [n]. Positions at the end of the string map
     * to [raw]'s length, so a half-open range converts without a special case.
     */
    fun rawAt(n: Int): Int = when {
        toRaw.isEmpty() -> raw.length
        n <= 0 -> toRaw.first()
        n >= toRaw.size -> raw.length
        else -> toRaw[n]
    }
}
