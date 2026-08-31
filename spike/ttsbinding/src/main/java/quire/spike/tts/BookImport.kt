package quire.spike.tts

import android.content.Context
import android.net.Uri
import java.io.File
import quire.attribution.Conversation
import quire.attribution.Heuristic
import quire.attribution.Roster
import quire.epub.EpubText
import quire.index.BookRecord
import quire.index.IndexWriter
import quire.index.Normalizer
import quire.index.Schema
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan
import quire.model.characters.CharacterManifest
import quire.model.characters.ManifestCodec

/**
 * Reads a book once and writes the app a note about it.
 *
 * This is the whole import: take the file, work out who is in it and how they speak, and
 * store that. Nothing here renders a book or keeps one — after this runs, what remains on
 * the device is a dialogue index and a cast, and the reader goes on reading in whatever app
 * they already use.
 *
 * The order matters and each step needs the one before it. The cast has to exist before
 * dialogue can be attributed to anyone; genders have to exist before the cast can be voiced;
 * and turn-taking needs Tier 1's named speakers before it has anything to alternate between.
 */
object BookImport {

    data class Result(
        val bookId: String,
        val title: String,
        val sentences: Int,
        val dialogueSpans: Int,
        val voiced: Int,
        val cast: List<String>,
    ) {
        /** The line the app shows when it says it is done. */
        val summary: String
            get() = "$title — ${cast.size} characters, $voiced of $dialogueSpans lines cast"
    }

    /** Where imported books live. One directory per book, named by its id. */
    fun library(context: Context) = File(context.filesDir, "books").apply { mkdirs() }

    fun imported(context: Context): List<File> =
        library(context).listFiles()?.filter { it.isDirectory }.orEmpty().sortedBy { it.name }

    /**
     * Import [uri], reporting progress so a long book does not look like a hang.
     *
     * Progress is coarse on purpose: the phases take wildly different times and a smooth
     * bar would be a lie. Naming the phase is more use than a percentage.
     */
    /** One line describing an imported book, read back from its own manifest. */
    fun describe(dir: File): String = runCatching {
        val manifest = ManifestCodec.decode(File(dir, "characters.json").readText())
        val cast = manifest.characters.joinToString(", ") { it.displayName }
        "${manifest.bookId} — ${manifest.characters.size} characters: $cast"
    }.getOrElse { "${dir.name} — unreadable (${it.message})" }

    fun run(context: Context, uri: Uri, progress: (String) -> Unit): Result {
        progress("copying the file")
        val staged = File(context.cacheDir, "import.epub")
        context.contentResolver.openInputStream(uri)?.use { input ->
            staged.outputStream().use(input::copyTo)
        } ?: error("could not open the file")

        progress("reading the book")
        val paragraphs = EpubText.paragraphs(staged)
        require(paragraphs.isNotEmpty()) { "no text found — is this an EPUB?" }

        progress("working out who is in it")
        val cast = Roster.scan(paragraphs.map { it.locator to it.text })
        val bookId = digest(staged)
        val manifest = Roster.manifest(cast, bookId, System.currentTimeMillis())

        progress("working out who says what")
        val tier1 = Heuristic(manifest).attributeAll(paragraphs.map { it.locator to it.text })
        val attributed = Conversation.resolve(tier1, cast = cast.names.toList())

        progress("writing the index")
        val entries = entries(paragraphs, attributed)
        val dir = File(library(context), bookId).apply { mkdirs() }
        File(dir, "characters.json").writeText(ManifestCodec.encode(manifest))
        val db = File(dir, "index.db")
        db.delete()
        AndroidSql(db.path, writable = true).use { sql ->
            IndexWriter(sql).apply {
                createSchema()
                write(
                    BookRecord(
                        bookId = bookId,
                        title = staged.nameWithoutExtension,
                        author = "imported",
                        entryCount = entries.size,
                        indexedAt = manifest.generatedAt,
                        schemaVersion = Schema.VERSION,
                        sourceDigest = bookId,
                    ),
                    entries,
                )
            }
        }
        staged.delete() // the book itself is not ours to keep

        val spans = entries.flatMap { it.spans }
        return Result(
            bookId = bookId,
            title = File(dir, "characters.json").let { manifest.bookId },
            sentences = entries.size,
            dialogueSpans = spans.count { it.kind == Kind.DIALOGUE },
            voiced = spans.count { it.speakerId != null },
            cast = manifest.characters.map { it.displayName },
        )
    }

    /**
     * Fold the attributed segments onto the sentences the index stores.
     *
     * Segments are runs within a paragraph; sentences are what a reader's app sends. Each
     * segment is located by walking the paragraph forward, because a short line of speech
     * can occur twice in one paragraph and only the first unconsumed one is this segment.
     */
    private fun entries(
        paragraphs: List<quire.model.Paragraph>,
        attributed: List<quire.model.AttributionResult>,
    ): List<IndexEntry> {
        val byParagraph = attributed.groupBy { it.locator.substringBeforeLast("#s") }
        val out = mutableListOf<IndexEntry>()
        for (paragraph in paragraphs) {
            val segments = byParagraph[paragraph.locator].orEmpty()
            var cursor = 0
            val placed = segments.mapNotNull { segment ->
                val needle = segment.text.trim()
                if (needle.isEmpty()) return@mapNotNull null
                val at = paragraph.text.indexOf(needle, cursor)
                if (at < 0) return@mapNotNull null
                cursor = at + needle.length
                Triple(at, at + needle.length, segment)
            }

            var offset = 0
            for (sentence in Normalizer.sentences(paragraph.text)) {
                val start = paragraph.text.indexOf(sentence, offset).takeIf { it >= 0 } ?: offset
                val end = start + sentence.length
                offset = end
                val spans = placed.mapNotNull { (from, to, segment) ->
                    val lo = maxOf(from, start)
                    val hi = minOf(to, end)
                    if (lo >= hi) null
                    else VoiceSpan(lo - start, hi - start, segment.kind, segment.speakerId, segment.confidence)
                }.ifEmpty { listOf(VoiceSpan(0, sentence.length, Kind.NARRATION, null, 0.0)) }

                out += IndexEntry(out.size, sentence, Normalizer.normalize(sentence), spans)
            }
        }
        return out
    }

    private fun digest(file: File): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
            .take(16)
}
