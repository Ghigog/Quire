package quire.spike.indexer

import java.io.File
import quire.index.Normalizer
import quire.model.characters.Gender
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

/**
 * Turns a labelled fixture into index entries.
 *
 * The fixture gives one speaker per sentence; the index needs spans *within* the sentence,
 * because `"I know," said Sarah.` is one entry carrying two voices. Splitting on quote
 * marks is fine **here** — this is the writer, working on whole sentences it can see the
 * ends of, which is exactly the context the TTS service does not have when the host hands
 * it a clause at a time (ADR-0002 §6). That asymmetry is the architecture: infer once,
 * offline, with the whole sentence; look up at play time.
 */
object Labelled {

    private const val NARRATION = "NARRATION"

    /**
     * The cast's genders, from a `# cast: Sarah=female, Thomas=male` header.
     *
     * It lives in the fixture rather than beside it because the book, the index and the
     * manifest are all generated from this one file, and a cast declared somewhere else
     * could disagree with the speakers actually used. QUI-007 replaces this with a real
     * scan; the shape it produces — `characters.json` — is already the frozen one.
     */
    fun cast(file: File): Map<String, Gender> =
        file.readLines()
            .firstOrNull { it.startsWith("# cast:") }
            ?.removePrefix("# cast:")
            ?.split(',')
            ?.mapNotNull { entry ->
                val (name, gender) = entry.split('=', limit = 2).map(String::trim)
                    .takeIf { it.size == 2 } ?: return@mapNotNull null
                name to Gender.from(gender)
            }
            ?.toMap()
            .orEmpty()

    /** `speaker<TAB>sentence`, `#` comments and blank lines ignored. */
    fun load(file: File): List<Pair<String?, String>> =
        file.readLines()
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { line ->
                val (label, text) = line.split('\t', limit = 2)
                (if (label == NARRATION) null else label) to text
            }

    fun entries(rows: List<Pair<String?, String>>, chapter: Int = 0): List<IndexEntry> =
        rows.mapIndexed { seq, (speaker, text) ->
            IndexEntry(
                seq = seq,
                text = text,
                normalized = Normalizer.normalize(text),
                spans = spans(text, speaker),
                chapter = chapter,
            )
        }

    /**
     * Split one sentence into alternating narration and speech runs.
     *
     * Confidence is 1.0 throughout: these are gold labels from a fixture, not a model's
     * guess. When QUI-007 writes real indexes the value carries the attributor's
     * confidence and PRD §2's gates apply to it.
     */
    fun spans(text: String, speaker: String?): List<VoiceSpan> {
        if (speaker == null) {
            return listOf(VoiceSpan(0, text.length, Kind.NARRATION, null, 1.0))
        }
        val out = mutableListOf<VoiceSpan>()
        var runStart = 0
        var inQuote = false
        text.forEachIndexed { i, c ->
            val opens = c == '“' || (c == '"' && !inQuote)
            val closes = c == '”' || (c == '"' && inQuote)
            when {
                closes && inQuote -> {
                    out += VoiceSpan(runStart, i + 1, Kind.DIALOGUE, speaker, 1.0)
                    runStart = i + 1
                    inQuote = false
                }
                opens && !inQuote -> {
                    if (i > runStart) out += VoiceSpan(runStart, i, Kind.NARRATION, null, 1.0)
                    runStart = i
                    inQuote = true
                }
            }
        }
        if (runStart < text.length) {
            // An unterminated quote runs to the end of the sentence: a speech continuing
            // into the next paragraph, which is the case that defeats inference at play
            // time and is harmless here because the span ends with the entry.
            val kind = if (inQuote) Kind.DIALOGUE else Kind.NARRATION
            out += VoiceSpan(runStart, text.length, kind, if (inQuote) speaker else null, 1.0)
        }
        return out
    }
}
