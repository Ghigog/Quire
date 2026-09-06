package quire.spike.bakeoff

import java.io.File
import quire.spike.ParagraphUnit

/**
 * A candidate whose answers were produced by something other than this JVM (QUI-028).
 *
 * The encoder candidates are PyTorch models with Python tokenisers, and reimplementing their
 * inference in Kotlin to keep everything in one process would put a rewrite of ours between
 * the published model and its published score. So the model runs in Python and this reads
 * what it wrote — **but the scoring stays here, in [Bakeoff], for every candidate alike.**
 * That is the whole point: what is being compared is models, not two scoring codebases that
 * happen to disagree about what counts as a match.
 *
 * The exchange is a directory:
 *
 * ```
 * <dir>/<novel>.paragraphs.jsonl   written by `bakeoff dump` — the text, and nothing else
 * <dir>/<novel>.questions.jsonl    written by `bakeoff dump` — where each quotation sits
 * <dir>/<novel>.answers.tsv        written by the predictor — id, speaker, evidence
 * ```
 *
 * **The dump carries no gold speakers.** A predictor that never sees the answer cannot score
 * itself by accident, which is the failure mode that cost QUI-018 its 100% precision.
 *
 * A missing file, a missing row or an empty speaker all mean the same thing: unanswered.
 * That is a legitimate result and [Bakeoff] scores it as coverage lost, not as a mistake.
 */
class ExternalCandidate(private val dir: File, override val id: String = "external") : Candidate {

    override val description: String
        get() = "answers read from ${dir.path}"

    override fun answer(
        paragraphs: List<ParagraphUnit>,
        questions: List<Question>,
    ): Map<String, Answer> {
        val novel = paragraphs.firstOrNull()?.locator?.substringBefore('#') ?: return emptyMap()
        val file = File(dir, "$novel.answers.tsv")
        if (!file.exists()) return emptyMap()

        val out = mutableMapOf<String, Answer>()
        file.forEachLine { line ->
            if (line.isBlank() || line.startsWith("#")) return@forEachLine
            val cells = line.split('\t')
            val speaker = cells.getOrNull(1)?.trim().orEmpty()
            if (speaker.isNotEmpty()) {
                out[cells[0]] = Answer(speaker, cells.getOrNull(2)?.trim().orEmpty().ifEmpty { id })
            }
        }
        return out
    }
}

/** Minimal JSON string escaping, so the dump does not pull in a serialisation dependency. */
internal fun jsonString(value: String): String {
    val sb = StringBuilder(value.length + 2).append('"')
    for (c in value) when {
        c == '"' -> sb.append("\\\"")
        c == '\\' -> sb.append("\\\\")
        c == '\n' -> sb.append("\\n")
        c == '\r' -> sb.append("\\r")
        c == '\t' -> sb.append("\\t")
        c < ' ' -> sb.append("\\u%04x".format(c.code))
        else -> sb.append(c)
    }
    return sb.append('"').toString()
}
