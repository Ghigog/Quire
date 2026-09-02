package quire.bakeoff

import java.io.File
import quire.model.Paragraph

/**
 * The Project Dialogism Novel Corpus, loaded so that every candidate is asked exactly the
 * same questions about exactly the same text (QUI-028).
 *
 * PDNC is not committed and carries no licence; `tools/fetch-pdnc.sh` clones it and
 * `docs/prior-art.md` §3 records why that is the only lawful way to use it.
 *
 * **Quotations are located by byte span, not by matching their text.** The first scoring
 * pass (QUI-018) keyed gold quotations against predicted segments by normalised text and
 * silently dropped every quotation that failed to key — it scored 2,846 of 37,131. PDNC
 * gives exact byte offsets into `novel_text.txt`; using them means the denominator is the
 * whole corpus and a quotation we cannot answer counts against us instead of vanishing.
 */
object Corpus {

    /** What PDNC's novel index says about a book, which is how holdouts are chosen. */
    data class NovelMeta(
        val folder: String,
        val title: String,
        /** 1 or 3. PDNC records this as a float. */
        val narrativePerson: Int,
        val translated: Boolean,
        val genre: String,
        val year: Int,
    )

    /**
     * One question put to a candidate: who speaks the text at these offsets?
     *
     * [start] and [end] are character offsets **within the paragraph**, so a candidate
     * never has to reason about the file. [text] is what PDNC recorded, for diagnostics
     * only — answering by matching it would be answering a different question.
     */
    data class Question(
        val id: String,
        val paragraph: Int,
        val start: Int,
        val end: Int,
        val text: String,
        /** `Explicit`, `Implicit`, `Anaphoric`, or `Unspecified` for PDNC's 14 blank rows. */
        val type: String,
        val gold: String,
    )

    data class Novel(
        val meta: NovelMeta,
        val paragraphs: List<Paragraph>,
        val questions: List<Question>,
        /** Quotations whose speaker is a PDNC pseudo-entity (`_group`, `_unknowable`). */
        val unscorable: Int,
        /** Quotations whose byte span fell outside every paragraph. Should be 0. */
        val unlocatable: Int,
    )

    /** PDNC marks a speaker nobody could name with a leading underscore. */
    private fun scorable(speaker: String) = speaker.isNotEmpty() && !speaker.startsWith("_")

    fun index(root: File): List<NovelMeta> {
        val file = File(root, "PDNC-Novel-Index.csv")
        require(file.exists()) { "not a PDNC checkout: ${root.path} (run tools/fetch-pdnc.sh)" }
        return Csv.parse(file.readText()).mapNotNull { row ->
            val folder = row["Folder Name"]?.trim().orEmpty()
            if (folder.isEmpty()) null
            else NovelMeta(
                folder = folder,
                title = row["Novel Title"]?.trim().orEmpty().ifEmpty { folder },
                narrativePerson = row["Narrative Person"]?.trim()?.toDoubleOrNull()?.toInt() ?: 3,
                translated = row["Translator Code"]?.trim().orEmpty().isNotEmpty(),
                genre = row["Genre"]?.trim().orEmpty().ifEmpty { "unknown" },
                year = row["Year of First Publication"]?.trim()?.toDoubleOrNull()?.toInt() ?: 0,
            )
        }
    }

    fun load(root: File, meta: NovelMeta): Novel {
        val dir = File(File(root, "data"), meta.folder)
        val bytes = File(dir, "novel_text.txt").readBytes()
        val text = String(bytes, Charsets.UTF_8)
        val offsets = ByteOffsets(text)

        val paragraphs = paragraphs(text, meta.folder)
        val questions = mutableListOf<Question>()
        var unscorable = 0
        var unlocatable = 0

        for (row in Csv.parse(File(dir, "quotation_info.csv").readText())) {
            val speaker = row["speaker"]?.trim().orEmpty()
            if (!scorable(speaker)) { unscorable++; continue }
            // A quotation split across paragraphs carries several spans. The first is where
            // it opens, and a speech tag sits beside the opening far more often than beside
            // a continuation, so that is the span every candidate is asked about.
            val span = spans(row["quoteByteSpans"].orEmpty()).firstOrNull() ?: run { unlocatable++; null } ?: continue
            val startChar = offsets.charOf(span.first)
            val endChar = offsets.charOf(span.second)
            val para = paragraphs.lastOrNull { it.start <= startChar } ?: run { unlocatable++; null } ?: continue
            if (startChar >= para.end) { unlocatable++; continue }

            val type = row["quoteType"]?.trim().orEmpty()
            questions += Question(
                id = "${meta.folder}/${row["quoteID"]?.trim().orEmpty()}",
                paragraph = para.index,
                start = startChar - para.start,
                end = minOf(endChar, para.end) - para.start,
                text = row["quoteText"].orEmpty().replace('\n', ' ').trim(),
                type = if (type.isEmpty() || type == "nan") "Unspecified" else type,
                gold = speaker,
            )
        }
        return Novel(meta, paragraphs.map { it.unit }, questions, unscorable, unlocatable)
    }

    /** A paragraph and where it sits in the novel text, in characters. */
    private data class Located(val unit: Paragraph, val index: Int, val start: Int, val end: Int)

    /**
     * Split on blank lines, the way PDNC's text is laid out.
     *
     * Line breaks inside a paragraph become single spaces rather than being collapsed away:
     * one character in, one character out, so an offset into the file is still an offset
     * into the paragraph. Collapsing runs of whitespace here — which is what the first
     * scorer did — would shift every offset after the first indented line.
     */
    private fun paragraphs(text: String, bookId: String): List<Located> {
        val out = mutableListOf<Located>()
        val flat = text.map { if (it == '\n' || it == '\r' || it == '\t') ' ' else it }.joinToString("")
        var i = 0
        var n = 0
        val breaks = Regex("\n[ \t]*\n")
        var searchFrom = 0
        while (searchFrom <= text.length) {
            val m = breaks.find(text, searchFrom)
            val end = m?.range?.first ?: text.length
            if (end > i && flat.substring(i, end).isNotBlank()) {
                out += Located(
                    unit = Paragraph("$bookId#p$n", flat.substring(i, end), chapterIndex = 0, index = n),
                    index = n, start = i, end = end,
                )
                n++
            }
            if (m == null) break
            i = m.range.last + 1
            searchFrom = i
        }
        return out
    }

    /** `[[2309, 2585], [2600, 2610]]` — PDNC writes these as Python literals. */
    internal fun spans(field: String): List<Pair<Int, Int>> =
        Regex("\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*]").findAll(field).map {
            it.groupValues[1].toInt() to it.groupValues[2].toInt()
        }.toList()

    /**
     * Byte offset to character offset.
     *
     * Every novel in the current revision is pure ASCII, so this is the identity today.
     * It exists because a corpus revision that adds one accented name would otherwise
     * shift every span after it by a character and be very hard to see in the numbers.
     */
    internal class ByteOffsets(text: String) {
        private val byteOfChar = IntArray(text.length + 1)

        init {
            var b = 0
            var i = 0
            while (i < text.length) {
                val cp = text.codePointAt(i)
                val n = Character.charCount(cp)
                repeat(n) { byteOfChar[i + it] = b }
                b += when {
                    cp < 0x80 -> 1
                    cp < 0x800 -> 2
                    cp < 0x10000 -> 3
                    else -> 4
                }
                i += n
            }
            byteOfChar[text.length] = b
        }

        fun charOf(byte: Int): Int {
            var lo = 0
            var hi = byteOfChar.size - 1
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (byteOfChar[mid] < byte) lo = mid + 1 else hi = mid
            }
            return lo
        }
    }
}
