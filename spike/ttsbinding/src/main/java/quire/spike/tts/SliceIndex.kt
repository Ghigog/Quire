package quire.spike.tts

import android.content.Context
import android.util.Log
import java.io.File
import quire.index.Matcher
import quire.index.SqliteBookIndex
import quire.spike.slice.Casting

/**
 * The pre-built index the vertical slice ships, plus the matcher and casting that read it.
 *
 * QUI-019 deliberately hardcodes the book: no fingerprinting, no companion app, no
 * indexing on device. That isolates the listening experience — which is what this spike is
 * for — from the performance of building an index, which is QUI-007's problem.
 *
 * Built by `tools/build-slice-index.sh`, not committed: it is a derived artefact, and the
 * fixture it derives from is the thing under version control.
 */
class SliceIndex private constructor(
    private val sql: AndroidSql,
    val matcher: Matcher,
    val casting: Casting,
    val title: String,
    val entries: Int,
) {
    /** Start over from the top of the book. The host has begun a new reading session. */
    fun rewind() = matcher.seek(0)

    fun close() = sql.close()

    companion object {
        private const val TAG = "QuireSlice"
        private const val ASSET = "slice-index.db"

        /**
         * Open the shipped index, or null if the build carries none — in which case the
         * probe falls back to reading everything in the narrator's voice, which is the
         * correct behaviour for an unindexed book (QUI-029) and makes a missing asset
         * obvious by ear rather than by crash.
         */
        fun open(context: Context, voiceCount: Int, narrator: Int): SliceIndex? = runCatching {
            // SQLite needs a real file, and an asset is a compressed stream, so it is
            // copied out once on first use.
            val file = File(context.filesDir, ASSET)
            if (!file.exists() || file.length() == 0L) {
                context.assets.open(ASSET).use { input ->
                    file.outputStream().use(input::copyTo)
                }
            }
            val sql = AndroidSql(file.path)
            val book = SqliteBookIndex.books(sql).first()
            val index = SqliteBookIndex(sql, book.bookId)

            // Every speaker the book contains, cast once. Doing it up front rather than
            // on demand keeps casting stable no matter which chapter is read first.
            val speakers = (0 until index.size)
                .mapNotNull(index::entry)
                .flatMap { entry -> entry.spans.mapNotNull { it.speakerId } }
                .distinct()

            val casting = Casting(speakers, voiceCount, narrator = narrator)
            Log.i(TAG, "index ${book.title}: ${index.size} entries, cast ${casting.cast}")
            SliceIndex(sql, Matcher(index), casting, book.title, index.size)
        }.onFailure { Log.w(TAG, "no slice index: ${it.message}") }.getOrNull()
    }
}
