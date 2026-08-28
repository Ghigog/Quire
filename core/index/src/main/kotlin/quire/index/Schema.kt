package quire.index

/**
 * `dialogue_index.db` — written once by the companion app, read read-only by the TTS
 * service (QUI-021).
 *
 * Granularity is the **sentence**, because that is the unit the host's chunks are built
 * from; matching below that granularity is the matcher's job via its intra-entry offset
 * (ADR-0004, QUI-022).
 */
object Schema {

    /**
     * Bumped whenever normalisation or the tables change. An index written by a different
     * version cannot be trusted to match, because [Normalizer] would have produced
     * different text, so it is rejected rather than migrated.
     */
    const val VERSION = 1

    val statements: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS books (
            book_id        TEXT PRIMARY KEY,
            title          TEXT NOT NULL,
            author         TEXT NOT NULL,
            entry_count    INTEGER NOT NULL,
            indexed_at     INTEGER NOT NULL,
            schema_version INTEGER NOT NULL,
            source_digest  TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS entries (
            book_id    TEXT NOT NULL,
            seq        INTEGER NOT NULL,
            text       TEXT NOT NULL,
            chapter    INTEGER NOT NULL,
            PRIMARY KEY (book_id, seq)
        ) WITHOUT ROWID
        """,
        """
        CREATE TABLE IF NOT EXISTS spans (
            book_id     TEXT NOT NULL,
            seq         INTEGER NOT NULL,
            ordinal     INTEGER NOT NULL,
            start       INTEGER NOT NULL,
            end         INTEGER NOT NULL,
            kind        INTEGER NOT NULL,
            speaker_id  TEXT,
            confidence  REAL NOT NULL,
            PRIMARY KEY (book_id, seq, ordinal)
        ) WITHOUT ROWID
        """,
        // One row per entry per prefix length. The relocation key: exact-equality lookups
        // only, never a range scan, and a short chunk can still find a long entry.
        //
        // WITHOUT ROWID throughout: every table here has a natural composite key, so the
        // default rowid layout would store the data once in the table and again in the
        // primary-key index. There is no separate index on (book_id, prefix_hash) either —
        // the primary key already begins with those columns, and adding one stored the
        // prefix data a third time.
        //
        // Hashed rather than stored as text. Six overlapping cumulative strings per
        // sentence cost more than the sentences do — measured at 7.6 MB for a 100k-word
        // novel against a 5 MB budget. Collisions are harmless because a candidate is
        // only a proposal that the matcher then verifies against the text.
        """
        CREATE TABLE IF NOT EXISTS prefixes (
            book_id     TEXT NOT NULL,
            prefix_hash INTEGER NOT NULL,
            seq         INTEGER NOT NULL,
            PRIMARY KEY (book_id, prefix_hash, seq)
        ) WITHOUT ROWID
        """,
    )
}
