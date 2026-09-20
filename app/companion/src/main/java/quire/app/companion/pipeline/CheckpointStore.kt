package quire.app.companion.pipeline

import java.io.File
import kotlinx.serialization.json.Json

/**
 * Persists one book's [ImportCheckpoint] to disk between runs.
 *
 * Same shape as [quire.model.characters.ManifestStore]: write to a temp file, rename into
 * place. A checkpoint half-written when the process died would resume from a state that
 * never really finished a scene — the rename is atomic on every filesystem Android uses, so
 * a reader sees either the previous checkpoint or the new one, never a partial write.
 *
 * [delete] is called once an import finishes: a finished book carries no checkpoint, and its
 * presence is exactly how [quire.app.companion.ImportService] tells an interrupted import
 * apart from one that never started.
 */
class CheckpointStore(private val dir: File) {

    fun write(checkpoint: ImportCheckpoint) {
        dir.mkdirs()
        val target = fileFor(checkpoint.bookId)
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(json.encodeToString(ImportCheckpoint.serializer(), checkpoint))
        check(temp.renameTo(target)) { "could not replace ${target.path}" }
    }

    fun read(bookId: String): ImportCheckpoint? {
        val file = fileFor(bookId)
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(ImportCheckpoint.serializer(), file.readText()) }.getOrNull()
    }

    fun delete(bookId: String) {
        fileFor(bookId).delete()
    }

    private fun fileFor(bookId: String) = File(dir, "$bookId.checkpoint.json")

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}
