package quire.spike.synth

import java.io.File
import quire.spike.ParagraphUnit
import quire.spike.Text
import quire.spike.Tier1

/**
 * The script `synthesize` writes (QUI-018's "a chapter becomes audio" scenario).
 *
 * Same split QUI-039's `ListenScript` already uses (CLAUDE.md §9): what is said and by whom
 * is decided here, in the JVM, where it has tests; `render.py` only turns the result into
 * sound. Tier 2/3 stays out of this — ADR-0001 is not yet Accepted — so a paragraph Tier 1
 * declines on is voiced by the narrator, which is architecture.md's documented fallback
 * rather than a gap this ticket papers over.
 */
object SynthesisScript {

    /** One span of the chapter in one voice. [speaker] is null for narration. */
    data class Piece(val text: String, val speaker: String?, val tier: String, val confidence: Double)

    data class Script(val book: String, val chapter: Int, val cast: List<String>, val pieces: List<Piece>)

    /**
     * The roster and the Tier 1 pass run over the whole book — a name is only stable if
     * attribution sees every chapter a character appears in — but the script keeps only
     * [chapterIndex]'s pieces, so the audio matches the chapter asked for.
     */
    fun build(book: String, units: List<ParagraphUnit>, chapterIndex: Int): Script {
        val roster = Tier1.bootstrapRoster(units)
        val results = Tier1.attribute(units, roster).associateBy { it.locator }

        val pieces = units
            .filter { it.chapterIndex == chapterIndex && it.text.isNotBlank() }
            .flatMap { unit -> Text.segment(unit) }
            .mapNotNull { seg -> results[seg.locator] }
            .filter { it.text.isNotBlank() }
            .map { Piece(it.text, it.speakerId, it.tier.name, it.confidence) }

        val cast = pieces.mapNotNull { it.speaker }.distinct().sorted()
        return Script(book, chapterIndex, cast, pieces)
    }

    fun write(script: Script, out: File) {
        out.parentFile?.mkdirs()
        out.printWriter().use { w ->
            w.println("{")
            w.println("""  "book": ${json(script.book)},""")
            w.println("""  "chapter": ${script.chapter},""")
            w.println("""  "cast": [${script.cast.joinToString(", ") { json(it) }}],""")
            w.println("""  "pieces": [""")
            script.pieces.forEachIndexed { i, p ->
                val comma = if (i == script.pieces.size - 1) "" else ","
                w.println(
                    """    {"text": ${json(p.text)}, "speaker": ${p.speaker?.let(::json) ?: "null"}, """ +
                        """"tier": ${json(p.tier)}, "confidence": ${"%.2f".format(p.confidence)}}$comma""",
                )
            }
            w.println("  ]")
            w.println("}")
        }
    }

    private fun json(value: String): String {
        val sb = StringBuilder(value.length + 2).append('"')
        for (c in value) when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c.code < 0x20 -> sb.append("\\u%04x".format(c.code))
            else -> sb.append(c)
        }
        return sb.append('"').toString()
    }
}
