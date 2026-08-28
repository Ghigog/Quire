package quire.index

import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

/**
 * [BookIndex] over `dialogue_index.db` (QUI-021).
 *
 * Opened by the TTS service against a read-only connection. Entries are cached lazily as
 * the cursor walks the book: matching touches the same handful of entries repeatedly, and
 * a whole novel's text does not belong resident in the service process.
 */
class SqliteBookIndex(
    private val sql: Sql,
    override val bookId: String,
) : BookIndex {

    override val size: Int = sql.query(
        "SELECT entry_count FROM books WHERE book_id = ?", listOf(bookId),
    ) { it.int(0) }.firstOrNull() ?: 0

    private val cache = LinkedHashMap<Int, IndexEntry>()

    override fun entry(seq: Int): IndexEntry? {
        if (seq < 0 || seq >= size) return null
        cache[seq]?.let { return it }

        val row = sql.query(
            "SELECT text, chapter FROM entries WHERE book_id = ? AND seq = ?",
            listOf(bookId, seq),
        ) { it.string(0) to it.int(1) }.firstOrNull() ?: return null

        val spans = sql.query(
            "SELECT start, end, kind, speaker_id, confidence FROM spans " +
                "WHERE book_id = ? AND seq = ? ORDER BY ordinal",
            listOf(bookId, seq),
        ) {
            VoiceSpan(
                start = it.int(0),
                end = it.int(1),
                kind = Kind.entries.getOrElse(it.int(2)) { Kind.NARRATION },
                speakerId = it.stringOrNull(3),
                confidence = it.double(4),
            )
        }

        // Recomputed rather than stored: see Schema's note on the size budget.
        val entry = IndexEntry(seq, row.first, Normalizer.normalize(row.first), spans, row.second)
        cache[seq] = entry
        if (cache.size > CACHE_ENTRIES) cache.remove(cache.keys.first())
        return entry
    }

    override fun seqsWithPrefix(prefix: String): List<Int> = sql.query(
        "SELECT seq FROM prefixes WHERE book_id = ? AND prefix_hash = ?",
        listOf(bookId, Normalizer.hash(prefix)),
    ) { it.int(0) }

    companion object {
        /**
         * Enough to hold the matcher's forward window and the entries either side of it
         * several times over, and small enough to be invisible against the audio buffers.
         */
        const val CACHE_ENTRIES = 64

        /** Books present in this database, with their metadata. */
        fun books(sql: Sql): List<BookRecord> = sql.query(
            "SELECT book_id, title, author, entry_count, indexed_at, schema_version, source_digest FROM books",
        ) {
            BookRecord(
                bookId = it.string(0), title = it.string(1), author = it.string(2),
                entryCount = it.int(3), indexedAt = it.long(4),
                schemaVersion = it.int(5), sourceDigest = it.string(6),
            )
        }
    }
}
