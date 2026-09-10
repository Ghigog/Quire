package quire.spike.listen

import java.io.File
import quire.spike.Pdnc
import quire.spike.bakeoff.Bakeoff
import quire.spike.bakeoff.Candidate
import quire.spike.bakeoff.Question

/**
 * The script a listening test is rendered from (QUI-039).
 *
 * Everything that decides *what is said and by whom* happens here, in the JVM, where a test
 * can reach it; `spike/hostbench/listen.py` only turns the result into sound. That split is
 * CLAUDE.md §9's rule, and it is the reason the passage chooser has tests and the synthesiser
 * does not.
 *
 * The output is one JSON file holding both renderings of the same prose. Two renderings rather
 * than two scripts, because the acceptance criterion is that the text and the cast are
 * *identical* between them and only the speaker differs — writing them side by side in one
 * file is how that stops being a promise and starts being the file format.
 */
object ListenScript {

    /** A stretch of a paragraph in one voice: narration, or a quotation with its two answers. */
    data class Piece(
        val paragraph: Int,
        val text: String,
        /** Null for narration. */
        val quotationId: String?,
        /** Who the first candidate gives this line to; null means the narrator reads it. */
        val a: String?,
        val b: String?,
    )

    data class Track(val name: String, val why: String, val pieces: List<Piece>)

    data class Script(
        val novel: String,
        val candidateA: String,
        val candidateB: String,
        val cast: List<String>,
        val tracks: List<Track>,
    )

    /**
     * Cut a paragraph into alternating narration and quotation.
     *
     * The quotation offsets come from PDNC, so this needs no segmenter of its own and cannot
     * disagree with the one the candidates were scored through. It matters for the listen:
     * `Marcus set his mug down. "We don't have enough time."` is a beat and a line, and reading
     * the beat in Marcus's voice is a different mistake from the one under test.
     *
     * **PDNC's spans stop inside the quotation marks**, so a naive cut leaves the opening `"` at
     * the end of one narration piece and the closing one at the start of the next — heard as a
     * stray mark read aloud, and a piece that is nothing but punctuation. The marks are pulled
     * into the quotation they belong to, and anything left holding no letters at all is dropped.
     */
    fun pieces(paragraph: Int, text: String, quotations: List<Question>): List<Piece> {
        val out = mutableListOf<Piece>()
        var at = 0
        for (q in quotations.sortedBy { it.start }) {
            var start = q.start.coerceIn(0, text.length)
            var end = q.end.coerceIn(start, text.length)
            if (start > 0 && text[start - 1] in QUOTES && start - 1 >= at) start--
            if (end < text.length && text[end] in QUOTES) end++
            if (start < at) continue // overlapping spans: keep the first, drop the rest
            if (start > at) add(out, paragraph, text.substring(at, start), null, null, null)
            add(out, paragraph, text.substring(start, end), q.id, null, null)
            at = end
        }
        if (at < text.length) add(out, paragraph, text.substring(at), null, null, null)
        return out
    }

    private fun add(into: MutableList<Piece>, paragraph: Int, text: String, id: String?, a: String?, b: String?) {
        // Nothing to say is not the same as nothing to render: a fragment of bare punctuation
        // reaches the synthesiser as a noise, so it is dropped rather than voiced.
        if (text.any { it.isLetterOrDigit() }) into += Piece(paragraph, text.trim(), id, a, b)
    }

    private val QUOTES = setOf('"', '\u201c', '\u201d')

    fun build(
        novelDir: File,
        a: Candidate,
        b: Candidate,
        quotations: Int,
        maxParagraphs: Int,
        passages: Int,
        disagreements: Int,
        context: Int,
        seed: Long,
    ): Script {
        val (located, gold) = Pdnc.locate(novelDir)
        // **Fold aliases before anything is cast.** Tier 1 names a speaker by whatever the tag
        // said, so one woman comes back as "Daisy Miller", "Miss Daisy" and "Miss Miller" — and
        // an unfolded cast would give her three voices, which the listener would hear as chaos
        // that has nothing to do with attribution. This is the same fold the scoring uses
        // (QUI-028's Pdnc.Identity); doing it here keeps the audio and the numbers agreeing on
        // who the characters are.
        val identity = Pdnc.identity(novelDir)
        fun who(name: String?) = name?.let { identity.character(it) ?: it }
        val paragraphs = located.map { it.unit }
        val (questions, _) = Bakeoff.questions(located, gold)
        val answersA = a.answer(paragraphs, questions)
        val answersB = b.answer(paragraphs, questions)

        // The chooser is handed positions and nothing else — see Passages.Spot.
        val spots = questions.map { Passages.Spot(it.id, it.paragraph, it.start, it.end) }
        val byParagraph = questions.groupBy { it.paragraph }
        val textOf = paragraphs.associate { it.index to it.text }

        val natural = Passages.natural(spots, quotations, maxParagraphs, passages, seed)
        val disagreeAt = questions
            // Folded on both sides: "Miss Daisy" against "Daisy Miller" is not a disagreement,
            // it is one woman with two names, and selecting on it would spend the listener's
            // attention on paragraphs where the two renderings sound identical.
            .filter { who(answersA[it.id]?.speaker) != who(answersB[it.id]?.speaker) }
            .map { it.paragraph }
            .toSet()
        val differing = Passages.aroundDisagreements(disagreeAt, context, disagreements, seed)

        fun render(passages: List<Passages.Passage>) = passages.flatMap { passage ->
            passage.paragraphs.flatMap { p ->
                val text = textOf[p] ?: return@flatMap emptyList()
                pieces(p, text, byParagraph[p].orEmpty()).map { piece ->
                    if (piece.quotationId == null) piece
                    else piece.copy(
                        a = who(answersA[piece.quotationId]?.speaker),
                        b = who(answersB[piece.quotationId]?.speaker),
                    )
                }
            }
        }

        val tracks = listOf(
            Track(
                "natural",
                "a continuous passage chosen without reference to either candidate's answers",
                render(natural),
            ),
            Track(
                "disagreements",
                "only the paragraphs where the two candidates part company, in context",
                render(differing),
            ),
        )
        // One cast for both renderings. Voice assignment is QUI-011/032/037's question and
        // letting it vary here would confound the only thing being listened for.
        val cast = tracks.flatMap { it.pieces }
            .flatMap { listOfNotNull(it.a, it.b) }
            .distinct()
            .sorted()
        return Script(novelDir.name, a.id, b.id, cast, tracks)
    }

    fun write(script: Script, out: File) {
        out.parentFile?.mkdirs()
        out.printWriter().use { w ->
            w.println("{")
            w.println("""  "novel": ${json(script.novel)},""")
            w.println("""  "candidateA": ${json(script.candidateA)},""")
            w.println("""  "candidateB": ${json(script.candidateB)},""")
            w.println("""  "cast": [${script.cast.joinToString(", ") { json(it) }}],""")
            w.println("""  "tracks": [""")
            script.tracks.forEachIndexed { i, track ->
                w.println("    {")
                w.println("""      "name": ${json(track.name)},""")
                w.println("""      "why": ${json(track.why)},""")
                w.println("""      "pieces": [""")
                track.pieces.forEachIndexed { j, piece ->
                    val comma = if (j == track.pieces.size - 1) "" else ","
                    w.println(
                        """        {"paragraph": ${piece.paragraph}, "text": ${json(piece.text)}, """ +
                            """"quotation": ${piece.quotationId?.let(::json) ?: "null"}, """ +
                            """"a": ${piece.a?.let(::json) ?: "null"}, """ +
                            """"b": ${piece.b?.let(::json) ?: "null"}}$comma"""
                    )
                }
                w.println("      ]")
                w.println(if (i == script.tracks.size - 1) "    }" else "    },")
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
