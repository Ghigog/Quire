package quire.index

import quire.model.IndexEntry

/** Metadata about an indexed book. */
data class BookRecord(
    val bookId: String,
    val title: String,
    val author: String,
    val entryCount: Int,
    val indexedAt: Long,
    val schemaVersion: Int,
    /** SHA-256 of the source EPUB, so a different edition of the same title is detectable. */
    val sourceDigest: String,
)

/**
 * Writes a book's index. Used only by the companion app (QUI-021).
 *
 * Deliberately separate from [BookIndex]: the TTS service takes the read interface and has
 * no way to reach this one. One writer, one reader, no locking problem.
 */
class IndexWriter(private val sql: Sql) {

    fun createSchema() {
        Schema.statements.forEach(sql::execute)
    }

    /**
     * Replace a book's index wholly, in one transaction.
     *
     * Whole-book rather than incremental because a partially written index is worse than
     * no index: the matcher would lock onto it and read half a book in the wrong voices.
     */
    fun write(book: BookRecord, entries: List<IndexEntry>) {
        require(book.schemaVersion == Schema.VERSION) {
            "refusing to write schema version ${book.schemaVersion}, this build writes ${Schema.VERSION}"
        }
        sql.transaction {
            for (table in listOf("prefixes", "spans", "entries", "books")) {
                sql.update("DELETE FROM $table WHERE book_id = ?", listOf(book.bookId))
            }
            sql.update(
                "INSERT INTO books (book_id, title, author, entry_count, indexed_at, schema_version, source_digest) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                listOf(
                    book.bookId, book.title, book.author, entries.size,
                    book.indexedAt, book.schemaVersion, book.sourceDigest,
                ),
            )
            for (entry in entries) {
                sql.update(
                    "INSERT INTO entries (book_id, seq, text, chapter) VALUES (?, ?, ?, ?)",
                    listOf(book.bookId, entry.seq, entry.text, entry.chapter),
                )
                entry.spans.forEachIndexed { ordinal, span ->
                    sql.update(
                        "INSERT INTO spans (book_id, seq, ordinal, start, end, kind, speaker_id, confidence) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        listOf(
                            book.bookId, entry.seq, ordinal, span.start, span.end,
                            span.kind.ordinal, span.speakerId, span.confidence,
                        ),
                    )
                }
                for (prefix in prefixesOf(entry.normalized)) {
                    sql.update(
                        "INSERT OR IGNORE INTO prefixes (book_id, prefix_hash, seq) VALUES (?, ?, ?)",
                        listOf(book.bookId, Normalizer.hash(prefix), entry.seq),
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Every one-to-[Normalizer.HEAD_WORDS]-word opening of an entry.
         *
         * The matcher probes progressively shorter prefixes, so all of them must be
         * stored — see [BookIndex.seqsWithPrefix] for why one fixed-width key fails.
         */
        fun prefixesOf(normalized: String): List<String> {
            val words = normalized.split(' ')
            return (1..minOf(Normalizer.HEAD_WORDS, words.size))
                .map { words.take(it).joinToString(" ") }
        }
    }
}
