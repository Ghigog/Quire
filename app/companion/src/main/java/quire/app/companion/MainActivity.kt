package quire.app.companion

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import quire.app.companion.pipeline.ImportProgress
import quire.app.companion.pipeline.ImportStage
import quire.app.companion.voice.CloudVoiceSettingsActivity
import quire.model.characters.ManifestStore

/**
 * QUI-025: the companion's only screen. Pick a book, watch it get ready, see its cast once
 * it is. Nothing here decides anything — [ImportService] runs the pipeline; this reads back
 * what it wrote and what it is doing right now.
 *
 * E-ink rules (CLAUDE.md §7): every refresh replaces the whole book list in one pass rather
 * than mutating rows in place, and the layout carries no animation to suppress in the first
 * place — see `res/values/themes.xml` and `res/layout/activity_main.xml`.
 */
class MainActivity : Activity() {

    private lateinit var bookList: LinearLayout
    private var service: ImportService? = null
    private var currentProgress: Pair<String, ImportProgress>? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val bound = (binder as ImportService.LocalBinder).service
            service = bound
            bound.setListener { bookId, progress ->
                currentProgress = bookId to progress
                runOnUiThread(::refresh)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Companion)
        setContentView(R.layout.activity_main)

        bookList = findViewById(R.id.book_list)
        findViewById<Button>(R.id.import_button).setOnClickListener { pickBook() }
        findViewById<Button>(R.id.cloud_voice_settings_button).setOnClickListener {
            startActivity(Intent(this, CloudVoiceSettingsActivity::class.java))
        }

        bindService(Intent(this, ImportService::class.java), connection, Context.BIND_AUTO_CREATE)
        resumeInterruptedImports()
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onDestroy() {
        service?.setListener(null)
        unbindService(connection)
        super.onDestroy()
    }

    private fun resumeInterruptedImports() {
        for (bookId in Library.interrupted(this)) {
            startService(Intent(this, ImportService::class.java).putExtra(ImportService.EXTRA_RESUME_BOOK_ID, bookId))
        }
    }

    private fun pickBook() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/epub+zip"
        }
        startActivityForResult(intent, PICK_EPUB_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_EPUB_REQUEST || resultCode != Activity.RESULT_OK) return
        val uri: Uri = data?.data ?: return
        startService(Intent(this, ImportService::class.java).putExtra(ImportService.EXTRA_URI, uri.toString()))
    }

    /**
     * Rebuilds the whole list in one pass — no partial mutation of existing rows — so a
     * refresh is a single batched change rather than a sequence e-ink would ghost on
     * (CLAUDE.md §7).
     */
    private fun refresh() {
        bookList.removeAllViews()
        val bookIds = Library.bookIds(this)
        if (bookIds.isEmpty()) {
            bookList.addView(textRow(getString(R.string.empty_library)))
            return
        }
        for (bookId in bookIds) {
            bookList.addView(rowFor(bookId))
        }
    }

    private fun rowFor(bookId: String): LinearLayout {
        val ready = Library.isReady(this, bookId)
        val (title, subtitle) = if (ready) {
            val manifest = ManifestStore(Library.root(this)).read(bookId)
            val cast = manifest?.characters?.joinToString(", ") { it.displayName }.orEmpty()
            (manifest?.bookId ?: bookId) to getString(R.string.status_ready).let { status ->
                if (cast.isEmpty()) status else "$status — $cast"
            }
        } else {
            val progress = currentProgress?.takeIf { it.first == bookId }?.second
            bookId to (progress?.let { stageLine(it) } ?: "queued")
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            setPadding(0, 16, 0, 16)
            addView(textRow(title).apply { textSize = 18f })
            addView(textRow(subtitle))
            if (ready) {
                addView(
                    Button(this@MainActivity).apply {
                        text = getString(R.string.cloud_voice_button)
                        setOnClickListener {
                            startActivity(
                                Intent(this@MainActivity, CloudVoiceSettingsActivity::class.java)
                                    .putExtra(CloudVoiceSettingsActivity.EXTRA_BOOK_ID, bookId),
                            )
                        }
                    },
                )
            }
        }
    }

    private fun stageLine(progress: ImportProgress): String {
        val stage = when (progress.stage) {
            ImportStage.COPYING -> "copying the file"
            ImportStage.PARSING -> "reading the book"
            ImportStage.SCANNING_CAST -> "working out who is in it"
            ImportStage.ATTRIBUTING_DIALOGUE -> "working out who says what"
            ImportStage.CASTING_VOICES -> "choosing voices"
            ImportStage.WRITING_INDEX -> "writing the index"
            ImportStage.DONE -> "done"
        }
        return getString(R.string.status_importing, stage, (progress.fraction * 100).toInt())
    }

    private fun textRow(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(getColor(R.color.ink_black))
    }

    private companion object {
        const val PICK_EPUB_REQUEST = 1
    }
}
