package quire.app.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import java.io.File
import java.security.MessageDigest
import quire.app.companion.pipeline.CheckpointStore
import quire.app.companion.pipeline.ImportPipeline
import quire.app.companion.pipeline.ImportProgress
import quire.app.companion.pipeline.ImportStage
import quire.app.companion.pipeline.IndexPublisher
import quire.attribution.slm.SlmRuntime
import quire.epub.EpubText
import quire.index.BookRecord
import quire.index.Schema
import quire.model.characters.ManifestStore
import quire.voice.foundry.SpeakerProfile

/**
 * QUI-025: runs one book's whole import — copy, parse, scan, attribute, cast, write — as a
 * foreground service, so it survives the companion app being backgrounded the way a 100k-word
 * book's worth of work needs to (PRD §2 Phase 1).
 *
 * Everything that decides anything lives in [ImportPipeline]; this class is the glue CLAUDE.md
 * §9 asks Android code to be — a driver, an asset copy, a notification. It reads the file,
 * calls the pipeline, and writes what comes back, atomically, through [ManifestStore] and
 * [IndexPublisher].
 *
 * **No SLM is wired in yet.** [slmRuntime] returns null: QUI-031 has not measured a runtime
 * on the reference device, so nothing has been picked to bind. Tier 1 and casting already
 * work without one; Tier 2/3 and trait enrichment stay dormant until a runtime exists to hand
 * [ImportPipeline] here — no other change in this file is needed when it does.
 */
class ImportService : Service() {

    private val binder = LocalBinder()
    @Volatile private var listener: ((bookId: String, ImportProgress) -> Unit)? = null
    @Volatile private var running = false

    inner class LocalBinder : Binder() {
        val service: ImportService get() = this@ImportService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    /** The activity calls this while it is on screen; there is never more than one listener. */
    fun setListener(listener: ((bookId: String, ImportProgress) -> Unit)?) {
        this.listener = listener
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        if (running) return START_NOT_STICKY
        val uriExtra = intent?.getStringExtra(EXTRA_URI)
        val resumeBookId = intent?.getStringExtra(EXTRA_RESUME_BOOK_ID)
        if (uriExtra == null && resumeBookId == null) return START_NOT_STICKY

        running = true
        Thread {
            try {
                if (uriExtra != null) importNew(Uri.parse(uriExtra)) else resume(resumeBookId!!)
            } finally {
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
        return START_NOT_STICKY
    }

    private fun importNew(uri: Uri) {
        val staged = File(cacheDir, "staged-${System.currentTimeMillis()}.epub")
        contentResolver.openInputStream(uri)?.use { input -> staged.outputStream().use(input::copyTo) }
            ?: error("could not open the selected file")

        val bookId = digest(staged)
        val bookDir = Library.bookDir(this, bookId)
        val keptEpub = File(bookDir, "book.epub")
        if (!keptEpub.exists()) staged.copyTo(keptEpub, overwrite = true)
        staged.delete()

        runPipeline(bookId, bookDir, keptEpub)
    }

    /** The app reopened after a kill mid-import: the staged copy is still in the book's own directory. */
    private fun resume(bookId: String) {
        val bookDir = Library.bookDir(this, bookId)
        val keptEpub = File(bookDir, "book.epub")
        if (!keptEpub.exists()) return
        runPipeline(bookId, bookDir, keptEpub)
    }

    private fun runPipeline(bookId: String, bookDir: File, epub: File) {
        report(bookId, ImportProgress(ImportStage.PARSING, 0.0))
        val paragraphs = EpubText.paragraphs(epub)
        require(paragraphs.isNotEmpty()) { "no text found — is this an EPUB?" }

        val checkpoints = CheckpointStore(Library.checkpointDir(this))
        val profile = assets.open(VOICE_PROFILE_ASSET).bufferedReader()
            .useLines { SpeakerProfile.parse(it) }

        val result = ImportPipeline(slm = structuredCompletion(), speakerProfile = profile).run(
            paragraphs = paragraphs,
            bookId = bookId,
            generatedAt = System.currentTimeMillis(),
            resumeFrom = checkpoints.read(bookId),
            onProgress = { report(bookId, it) },
            onCheckpoint = checkpoints::write,
        )

        report(bookId, ImportProgress(ImportStage.WRITING_INDEX, 0.0))
        ManifestStore(Library.root(this)).write(result.manifest)
        IndexPublisher.publish(
            dbFile = File(bookDir, "index.db"),
            book = BookRecord(
                bookId = bookId,
                title = epub.nameWithoutExtension,
                author = "imported",
                entryCount = result.entries.size,
                indexedAt = result.manifest.generatedAt,
                schemaVersion = Schema.VERSION,
                sourceDigest = bookId,
            ),
            entries = result.entries,
            open = ::AndroidSql,
        )
        checkpoints.delete(bookId)
        epub.delete() // the book itself is not ours to keep (CLAUDE.md §8) — only the note we wrote about it
        report(bookId, ImportProgress(ImportStage.DONE, 1.0))
    }

    private fun structuredCompletion(): quire.attribution.slm.StructuredCompletion? =
        slmRuntime()?.let { quire.attribution.slm.StructuredCompletion(it) }

    private fun slmRuntime(): SlmRuntime? = null

    private fun report(bookId: String, progress: ImportProgress) {
        listener?.invoke(bookId, progress)
    }

    private fun startForegroundNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.import_channel_name), NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification = notification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.importing_notification))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

    private fun digest(file: File): String =
        MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
            .take(16)

    companion object {
        const val EXTRA_URI = "quire.app.companion.URI"
        const val EXTRA_RESUME_BOOK_ID = "quire.app.companion.RESUME_BOOK_ID"
        private const val VOICE_PROFILE_ASSET = "voices/libritts_r-f0.tsv"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "import"
    }
}
