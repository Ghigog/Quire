package quire.spike.indexer

import java.io.File
import java.security.MessageDigest
import kotlin.system.exitProcess
import quire.index.BookRecord
import quire.index.IndexWriter
import quire.index.Matcher
import quire.index.Schema
import quire.index.SqliteBookIndex
import quire.model.Kind

private const val USAGE = """
quire-indexer-spike (QUI-019)

  build <labelled.tsv> <out.db>     write a dialogue index the TTS service can read
  replay <index.db> <trace.tsv>     replay a host trace against an index, resolving voices

The trace is a QUI-020 capture: one host chunk per row, `text` in the last column.
"""

fun main(args: Array<String>) {
    if (args.size < 3) { println(USAGE.trim()); exitProcess(2) }
    when (args[0]) {
        "build" -> build(File(args[1]), File(args[2]))
        "replay" -> replay(File(args[1]), File(args[2]))
        else -> { println(USAGE.trim()); exitProcess(2) }
    }
}

private fun build(fixture: File, out: File) {
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

private fun digest(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
