package quire.app.companion

import android.content.Context
import java.io.File

/**
 * Where imported books live on disk.
 *
 * One directory per book, named by its id, so two imports never write over each other's
 * bytes. A book is "ready" — visible to the TTS service and worth listing here — only once
 * both `index.db` and its manifest exist; `characters.json` lives one level up, shared with
 * [quire.model.characters.ManifestStore]'s own per-book naming, since the manifest store
 * already owns that layout (QUI-005).
 */
object Library {

    fun root(context: Context): File = File(context.filesDir, "books").apply { mkdirs() }

    fun bookDir(context: Context, bookId: String): File = File(root(context), bookId).apply { mkdirs() }

    fun checkpointDir(context: Context): File = File(context.filesDir, "checkpoints").apply { mkdirs() }

    fun isReady(context: Context, bookId: String): Boolean =
        File(bookDir(context, bookId), "index.db").exists()

    /** Every book directory ever created, ready or not — a partial import still has one. */
    fun bookIds(context: Context): List<String> =
        root(context).listFiles { f: File -> f.isDirectory }.orEmpty().map { it.name }.sorted()

    /** A book whose directory exists, is not ready, and still has its staged EPUB — an import a kill interrupted. */
    fun interrupted(context: Context): List<String> =
        bookIds(context).filter { !isReady(context, it) && File(bookDir(context, it), "book.epub").exists() }
}
