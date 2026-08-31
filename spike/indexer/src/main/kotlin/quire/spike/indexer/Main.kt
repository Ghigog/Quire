package quire.spike.indexer

import java.io.File
import java.security.MessageDigest
import kotlin.system.exitProcess
import quire.index.BookRecord
import quire.index.InMemoryBookIndex
import quire.index.IndexWriter
import quire.index.Matcher
import quire.index.Schema
import quire.index.SqliteBookIndex
import quire.model.Kind
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender
import quire.model.characters.ManifestCodec
import quire.spike.slice.Casting
import quire.spike.slice.ChunkPlan
import quire.spike.slice.VoiceProfile

private const val USAGE = """
quire-indexer-spike (QUI-019)

  build <labelled.tsv> <out.db>     write a dialogue index the TTS service can read
                                    add --epub <out.epub> to emit the matching book
  replay <index.db> <trace.tsv>     replay a host trace against an index, resolving voices
  read <labelled.tsv>               read the book the way a host does, printing each
                                    chunk's voice — the slice, without a device
  book <attributed.tsv> <out.db>    index a REAL book from the pipeline's export, using
                                    the characters.json written beside it

The trace is a QUI-020 capture: one host chunk per row, `text` in the last column.
"""

fun main(args: Array<String>) {
    if (args.size < 2) { println(USAGE.trim()); exitProcess(2) }
    if (args[0] != "read" && args.size < 3) { println(USAGE.trim()); exitProcess(2) }
    val epub = args.indexOf("--epub").takeIf { it > 0 }?.let { File(args[it + 1]) }
    when (args[0]) {
        "build" -> build(File(args[1]), File(args[2]), epub)
        "replay" -> replay(File(args[1]), File(args[2]))
        "read" -> read(File(args[1]))
        "book" -> book(File(args[1]), File(args[2]))
        else -> { println(USAGE.trim()); exitProcess(2) }
    }
}

private fun build(fixture: File, out: File, epub: File? = null) {
    val rows = Labelled.load(fixture)
    val entries = Labelled.entries(rows)
    out.delete()
    Jdbc(out.path).use { sql ->
        val writer = IndexWriter(sql)
        writer.createSchema()
        writer.write(
            BookRecord(
                bookId = digest(fixture.readBytes()).take(16),
                title = fixture.nameWithoutExtension,
                author = "fixture",
                entryCount = entries.size,
                indexedAt = System.currentTimeMillis(),
                schemaVersion = Schema.VERSION,
                sourceDigest = digest(fixture.readBytes()),
            ),
            entries,
        )
    }
    val speakers = entries.flatMap { it.spans }.mapNotNull { it.speakerId }.distinct().sorted()
    println("indexed ${entries.size} entries from ${fixture.name}")
    println("  speakers   ${speakers.joinToString(", ").ifEmpty { "none" }}")
    println("  dialogue   ${entries.flatMap { it.spans }.count { it.kind == Kind.DIALOGUE }} spans")
    println("  wrote      ${out.path} (${out.length()} bytes)")

    // The cast, in QUI-005's frozen shape. The service needs it to know that Sarah is a
    // woman; without it, casting is distinct-but-arbitrary and the device test heard a
    // male Sarah. QUI-007 will generate this from a scan instead of a fixture header.
    val cast = Labelled.cast(fixture)
    val manifest = File(out.parentFile, "characters.json")
    manifest.writeText(ManifestCodec.encode(CharacterManifest(
        schemaVersion = CharacterManifest.VERSION,
        bookId = digest(fixture.readBytes()).take(16),
        generatedAt = System.currentTimeMillis(),
        narrator = Character(
            id = "narrator", displayName = "Narrator",
            gender = cast["narrator"] ?: Gender.NEUTRAL, confidence = 1.0,
        ),
        characters = speakers.map { name ->
            Character(
                id = name, displayName = name,
                gender = cast[name] ?: Gender.UNKNOWN, confidence = 1.0,
                lineCount = entries.count { e -> e.spans.any { it.speakerId == name } },
            )
        },
    )))
    println("  cast       ${manifest.path} (${cast.entries.joinToString { "${it.key}=${it.value.name.lowercase()}" }})")

    // The book and the index come out of one fixture on purpose: if they could drift, a
    // wrong voice on device would be ambiguous between a bad match and a bad fixture.
    epub?.let {
        Epub.write(rows, it, fixture.nameWithoutExtension)
        println("  book       ${it.path} (${it.length()} bytes)")
    }
}

/**
 * The question this whole architecture exists to answer: given the clauses a host actually
 * sends, does the index say who is speaking?
 *
 * Prints one line per chunk with the voice it resolves to, so a wrong answer is visible
 * rather than merely audible.
 */
private fun replay(db: File, trace: File) {
    Jdbc(db.path).use { sql ->
        val book = SqliteBookIndex.books(sql).first()
        val index = SqliteBookIndex(sql, book.bookId)
        val matcher = Matcher(index)
        println("replaying ${trace.name} against ${book.title} (${index.size} entries)\n")

        var resolved = 0
        var total = 0
        for (line in trace.readLines().drop(1)) {
            if (line.isBlank()) continue
            val text = line.substringAfterLast('\t')
            if (text.isBlank()) continue
            total++
            val result = matcher.match(text)
            val voices = result.spans
                .joinToString(" | ") { it.speakerId ?: "narrator" }
                .ifEmpty { "—" }
            if (result.matched) resolved++
            println("%-9s %-26s %s".format(
                result.how, "\"" + text.trim().take(24) + "\"", voices))
        }
        println("\nresolved $resolved of $total chunks")
    }
}

/**
 * Index a real book from `quire-pipeline-spike export`.
 *
 * The cast comes from the `characters.json` the exporter wrote beside the TSV rather than
 * being re-derived here: genders decide how the book sounds, and deriving them twice is
 * how two answers appear.
 */
private fun book(tsv: File, out: File) {
    val entries = Attributed.entries(tsv)
    require(entries.isNotEmpty()) { "no entries in ${tsv.path}" }

    val castFile = File(tsv.parentFile ?: File("."), "characters.json")
    require(castFile.exists()) { "expected ${castFile.path} beside the export" }
    val manifest = ManifestCodec.decode(castFile.readText())

    out.delete()
    Jdbc(out.path).use { sql ->
        val writer = IndexWriter(sql)
        writer.createSchema()
        writer.write(
            BookRecord(
                bookId = manifest.bookId.take(16),
                title = manifest.bookId,
                author = "scanned",
                entryCount = entries.size,
                indexedAt = manifest.generatedAt,
                schemaVersion = Schema.VERSION,
                sourceDigest = digest(tsv.readBytes()),
            ),
            entries,
        )
    }

    val voiced = entries.flatMap { it.spans }.count { it.speakerId != null }
    val dialogue = entries.flatMap { it.spans }.count { it.kind == Kind.DIALOGUE }
    println("indexed ${entries.size} sentences from ${tsv.name}")
    println("  cast       ${manifest.characters.joinToString { "${it.id}=${it.gender.name.lowercase()}" }}")
    println("  dialogue   $dialogue spans, $voiced with a speaker")
    println("  wrote      ${out.path} (${out.length()} bytes)")
    println()
    println("Copy it to spike/ttsbinding/src/main/assets/slice-index.db, with characters.json,")
    println("and rebuild the probe to read this book instead of the fixture.")
}

/**
 * Read the book as the host will, and print who says each clause.
 *
 * The chunking mirrors the captured trace: split at commas, separator kept on the front of
 * the continuation. Every row with a speaker and no quote mark in it is a chunk that
 * quote-mark inference would have read in the narrator's voice.
 */
private fun read(fixture: File) {
    val rows = Labelled.load(fixture)
    val entries = Labelled.entries(rows)
    val cast = Labelled.cast(fixture)
    val profileFile = File(fixture.parentFile.parentFile, "voices/libritts_r-f0.tsv")
    val profile = profileFile.takeIf { it.exists() }
        ?.useLines { VoiceProfile.parse(it) }
    val speakers = entries.flatMap { e -> e.spans.mapNotNull { it.speakerId } }.distinct()
    val casting = Casting(
        speakers.associateWith { cast[it] ?: Gender.UNKNOWN },
        voiceCount = 904,
        profile = profile,
        narratorGender = cast["narrator"] ?: Gender.NEUTRAL,
    )
    val matcher = Matcher(InMemoryBookIndex(fixture.nameWithoutExtension, entries))

    println("reading ${fixture.name}")
    println("  voices: ${if (profile == null) "no profile — arbitrary" else "${profile.size} profiled"}")
    println("  narrator=${casting.narrator}, ${casting.cast.entries.joinToString { "${it.key}=${it.value}" }}\n")
    var rescued = 0
    for (entry in entries) {
        for (chunk in hostChunks(entry.text)) {
            val segments = ChunkPlan.of(chunk, matcher.match(chunk), casting)
            val voices = segments.joinToString(" | ") {
                "${it.speakerId ?: "narrator"}(${it.voice})"
            }
            val quoteless = '"' !in chunk && segments.any { it.speakerId != null }
            if (quoteless) rescued++
            println("%-1s %-58s %s".format(
                if (quoteless) "!" else " ", "\"" + chunk.trim().take(56) + "\"", voices))
        }
    }
    println("\n! = a chunk with no quote mark in it that is still dialogue: $rescued of them.")
    println("    Those are the chunks quote-mark inference reads in the wrong voice.")
}

/** Clause-level splitting of the shape `fixtures/host-traces` captured. */
private fun hostChunks(text: String): List<String> {
    val out = mutableListOf<String>()
    var start = 0
    text.forEachIndexed { i, c ->
        if (c == ',' && i + 1 < text.length) { out += text.substring(start, i + 1); start = i + 1 }
    }
    out += text.substring(start)
    return out.filter { it.isNotBlank() }
}

private fun digest(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
