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
     * Every seq whose head — the first [Normalizer.HEAD_WORDS] words of its normalised
     * text, or all of them if it is shorter — equals [head] exactly.
     *
     * The matcher probes with progressively shorter word prefixes, so an implementation
     * only ever needs exact equality on a stored key, never a range scan.
     *
     * This is the relocation path: PRD §2 Phase 2's "hash lookup", expressed as intent so
     * QUI-021 can index it however SQLite prefers (a stored, indexed `head` column is the
     * obvious implementation). Keyed on the head rather than the whole entry because a
     * host chunk is rarely equal to any single entry — it glues several together — but its
     * head is always the head of the entry it starts on.
     *
     * Order is unspecified; the matcher picks by distance from the cursor.
     */
    fun seqsWithHead(head: String): List<Int>
}

class InMemoryBookIndex(
    override val bookId: String,
    private val entries: List<IndexEntry>,
) : BookIndex {

    private val byHead: Map<String, List<Int>> =
        entries.groupBy({ Normalizer.head(it.normalized) }, { it.seq })

    override val size get() = entries.size

    override fun entry(seq: Int) = entries.getOrNull(seq)

    override fun seqsWithHead(head: String) = byHead[head].orEmpty()
}
