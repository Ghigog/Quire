package quire.app.companion.pipeline

import java.io.File
import quire.index.BookRecord
import quire.index.IndexWriter
import quire.index.Sql
import quire.model.IndexEntry

/**
 * Writes `dialogue_index.db` to a temp file and renames it into place — the same discipline
 * [quire.model.characters.ManifestStore] holds for `characters.json`, and for the same
 * reason: [quire.app.companion.ImportService] is the only writer and the TTS service is a
 * reader with no lock between them, so a partial index must never be visible at the real
 * path (QUI-025's own acceptance criterion).
 *
 * Generic over the [Sql] implementation so this runs identically against
 * [quire.app.companion.AndroidSql] on-device and against a JDBC driver in a JVM test — the
 * platform-specific half is exactly one open call, everything else is [IndexWriter].
 */
object IndexPublisher {

    fun <T> publish(dbFile: File, book: BookRecord, entries: List<IndexEntry>, open: (path: String) -> T)
        where T : Sql, T : AutoCloseable {
        val temp = File(dbFile.parentFile, "${dbFile.name}.tmp")
        temp.delete()
        open(temp.path).use { sql ->
            IndexWriter(sql).apply {
                createSchema()
                write(book, entries)
            }
        }
        check(temp.renameTo(dbFile)) { "could not publish ${dbFile.path}" }
    }
}
