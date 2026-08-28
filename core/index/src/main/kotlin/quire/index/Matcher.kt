package quire.index

import quire.model.IndexEntry
import quire.model.VoiceSpan

/** How a chunk was placed in the book. */
enum class How {
    /** Matched at or just after the cursor — the normal case while reading forward. */
    FORWARD,

    /** The cursor was wrong and the chunk was found elsewhere: a seek, or first play. */
    RELOCATED,

    /** Not in this book's index. The caller reads it in the narrator voice. */
    NONE,
}

/**
 * What the TTS service needs to voice one incoming chunk.
 *
 * [spans] is the ordered list of voiced runs across all matched entries, with offsets
 * rebased onto the chunk the host actually supplied — so the service can emit
 * `rangeStart` against the string it was handed rather than against our index.
 */
data class MatchResult(
    val how: How,
    val entries: List<IndexEntry>,
    val spans: List<VoiceSpan>,
    /** True when the host split a sentence and this chunk is only part of one entry. */
    val partial: Boolean = false,
) {
    val matched get() = how != How.NONE

    companion object {
        val none = MatchResult(How.NONE, emptyList(), emptyList())
    }
}

/**
 * Places an incoming chunk of text in a book by tracking a cursor, falling back to lookup
 * when the cursor is wrong.
 *
 * **Why not a plain hash table.** Text to speaker is many-to-many: `"Well,"` is spoken by
 * two different characters within twenty lines of one fixture, and novels are dense with
 * bare `"Yes."` and `"I know."`. A hash lookup answers confidently and wrongly on exactly
 * the rapid dialogue this product exists to voice. Reading is sequential, so a cursor
 * resolves those collisions for free and the lookup becomes the seek-recovery path.
 *
 * Not thread-safe: one matcher per TTS session, driven from the binder thread.
 */
class Matcher(
    private val index: BookIndex,
    /** How far ahead of the cursor to look before giving up and relocating. */
    private val window: Int = DEFAULT_WINDOW,
) {

    /** Last entry consumed, or -1 before the first match. */
    var cursor: Int = -1
        private set

    fun match(chunk: String): MatchResult {
        val wanted = Normalizer.normalize(chunk)
        if (wanted.isEmpty()) return MatchResult.none

        // Forward from the cursor. Starting at `cursor` rather than `cursor + 1` costs
        // nothing and absorbs a host that re-reads the last sentence after a page turn.
        // Skipped entirely before the first match: an unpositioned cursor is not a
        // position, and scanning from 0 would masquerade as reading forward.
        if (cursor >= 0) {
            for (start in cursor..(cursor + window)) {
                runAt(start, wanted)?.let { return accept(it, How.FORWARD) }
            }
        }

        // The cursor is wrong: a seek, a jump, or the first chunk of the session.
        // Candidates are entries the chunk could start on; when several collide, the
        // nearest to where we were is overwhelmingly the right one.
        val candidates = candidateStarts(wanted)
            .sortedBy { if (cursor < 0) it else kotlin.math.abs(it - cursor) }
        for (start in candidates) {
            runAt(start, wanted)?.let { return accept(it, How.RELOCATED) }
        }

        return MatchResult.none
    }

    /**
     * Entries the chunk could begin on.
     *
     * Looked up on progressively shorter heads rather than one fixed-width key, because a
     * short entry — a heading, a bare `"Yes."` — has fewer words than the key width and
     * would never be found by a six-word probe. Longest first, so the most specific
     * candidates are considered before the vaguest.
     */
    private fun candidateStarts(normalized: String): List<Int> {
        val words = normalized.split(' ')
        val out = LinkedHashSet<Int>()
        for (n in minOf(Normalizer.HEAD_WORDS, words.size) downTo 1) {
            out += index.seqsWithHead(words.take(n).joinToString(" "))
        }
        return out.toList()
    }

    /** Force the cursor, e.g. after QUI-023 identifies the book from a known position. */
    fun seek(seq: Int) {
        cursor = seq.coerceIn(-1, index.size - 1)
    }

    /**
     * Try to consume the whole of [wanted] as a contiguous run of entries from [start].
     *
     * Consumption is by concatenation rather than sentence-by-sentence, because the host's
     * boundaries are not ours. NeoReader segments on terminal punctuation and not on
     * document structure, so a chapter heading with no full stop arrives glued to the
     * sentence beneath it as a single chunk with *fewer* boundaries than the index has
     * (ADR-0004). Splitting the chunk could never recover the boundary the index knows
     * about; walking entries and eating their text off the front of the chunk does.
     */
    private fun runAt(start: Int, wanted: String): Run? {
        val consumed = mutableListOf<IndexEntry>()
        var remaining = wanted
        var i = 0
        while (remaining.isNotEmpty()) {
            val entry = index.entry(start + i) ?: return null
            val text = entry.normalized
            when {
                remaining == text -> {
                    consumed += entry
                    remaining = ""
                }
                remaining.startsWith("$text ") -> {
                    consumed += entry
                    remaining = remaining.substring(text.length + 1)
                }
                // The chunk stops inside this entry: the host split a sentence at its
                // character limit, so we have a fragment of it.
                text.startsWith(remaining) -> {
                    consumed += entry
                    return Run(consumed, partial = true)
                }
                else -> return null
            }
            i++
        }
        return if (consumed.isEmpty()) null else Run(consumed, partial = false)
    }

    private fun accept(run: Run, how: How): MatchResult {
        // A partial match leaves the cursor on the entry still being spoken, so the rest
        // of that sentence matches forward next time.
        cursor = if (run.partial) run.entries.first().seq else run.entries.last().seq
        return MatchResult(how, run.entries, rebase(run.entries), run.partial)
    }

    /**
     * Rebase span offsets from per-entry onto the concatenated chunk, so callers can map
     * them back to the host's own string.
     */
    private fun rebase(entries: List<IndexEntry>): List<VoiceSpan> {
        val out = mutableListOf<VoiceSpan>()
        var base = 0
        for (entry in entries) {
            entry.spans.mapTo(out) { it.copy(start = it.start + base, end = it.end + base) }
            base += entry.text.length + 1 // the space the host puts between sentences
        }
        return out
    }

    private data class Run(val entries: List<IndexEntry>, val partial: Boolean)

    companion object {
        /**
         * Hosts skip headings, page numbers and footnotes unpredictably, so the matcher
         * looks a few entries ahead before concluding the cursor is wrong.
         */
        const val DEFAULT_WINDOW = 5
    }
}
