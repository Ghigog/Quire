package quire.index

import quire.model.IndexEntry

/**
 * Read-only view of one book's index.
 *
 * The port the matcher needs, nothing more. QUI-021 implements it over
 * `dialogue_index.db`; [InMemoryBookIndex] implements it for tests. Deliberately
 * read-only: the TTS service must not be able to corrupt an index the companion app owns.
 */
interface BookIndex {
    val bookId: String
    val size: Int

    /** The entry at [seq], or null if out of range. */
    fun entry(seq: Int): IndexEntry?

    /**
     * Every seq whose normalised text *begins with* the word sequence [prefix], where
     * [prefix] is between one and [Normalizer.HEAD_WORDS] words.
     *
     * This is the relocation path — PRD §2 Phase 2's "hash lookup", expressed as intent
     * so QUI-021 can store it however SQLite prefers. Exact-equality lookups rather than
     * a range scan: store one row per entry per prefix length, so a six-word-or-longer
     * entry contributes six keys.
     *
     * One fixed-width key does not work in either direction. A short entry — a heading, a
     * bare `"Yes."` — has fewer words than the key. And a short chunk cannot produce the
     * six-word key of the longer sentence it starts, which is the common case now that we
     * know hosts chunk by clause (ADR-0004).
     *
     * Order is unspecified; the matcher picks by distance from the cursor.
     */
    fun seqsWithPrefix(prefix: String): List<Int>
}

class InMemoryBookIndex(
    override val bookId: String,
    private val entries: List<IndexEntry>,
) : BookIndex {

    // `run` rather than `apply`/`buildMap`: with the map as receiver, `entries` would
    // resolve to Map.entries instead of the constructor parameter.
    private val byPrefix: Map<String, List<Int>> = run {
        val map = mutableMapOf<String, MutableList<Int>>()
        for (indexed in entries) {
            val words = indexed.normalized.split(' ')
            for (n in 1..minOf(Normalizer.HEAD_WORDS, words.size)) {
                map.getOrPut(words.take(n).joinToString(" ")) { mutableListOf() } += indexed.seq
            }
        }
        map
    }

    override val size get() = entries.size

    override fun entry(seq: Int) = entries.getOrNull(seq)

    override fun seqsWithPrefix(prefix: String) = byPrefix[prefix].orEmpty()
}
