package quire.spike

import java.io.File
import quire.attribution.Roster
import quire.model.characters.Gender

/**
 * Loads the Project Dialogism Novel Corpus and scores against it (QUI-018, QUI-028, QUI-032).
 *
 * PDNC labels every quotation `Explicit` (a named speech tag), `Anaphoric` (a pronoun tag)
 * or `Implicit` (no tag). That split is exactly our tier boundary, so scoring against it
 * says not just how accurate Tier 1 is but how much of a real book it can *ever* reach.
 *
 * The corpus carries no licence and is not committed. `tools/fetch-pdnc.sh` clones it; see
 * `docs/prior-art.md` §3 for why that is the only lawful way to use it.
 *
 * **Quotations are located by byte span, not by matching their text.** PDNC gives exact
 * offsets into `novel_text.txt` and this uses them. Until 2026-09-02 the scoring here keyed
 * gold quotations against predicted segments by normalised text and counted only the ones
 * that keyed — 2,846 of 37,131, self-selected for being short and cleanly punctuated — so
 * every number it produced was measured on 7.7% of the corpus. Offsets mean the denominator
 * is the whole corpus and a quotation we cannot answer counts against us instead of
 * vanishing.
 */
object Pdnc {

    /** What PDNC's own index says about a book, which is how the holdouts are chosen. */
    data class NovelMeta(
        val folder: String,
        val title: String,
        /** 1 or 3. PDNC records this as a float. */
        val narrativePerson: Int,
        val translated: Boolean,
        val genre: String,
        val year: Int,
    )

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

    /**
     * One annotated quotation.
     *
     * [spans] are byte offsets into `novel_text.txt`. A quotation broken across paragraphs
     * carries several; the first is where it opens, and a speech tag sits beside the opening
     * far more often than beside a continuation.
     */
    data class Gold(
        val id: String,
        val text: String,
        val speaker: String,
        val type: String,
        val spans: List<Pair<Int, Int>>,
    ) {
        /** PDNC marks a speaker nobody could name with a leading underscore. */
        val scorable get() = speaker.isNotEmpty() && !speaker.startsWith("_")
    }

    /** A paragraph and where it sits in the novel text, in characters. */
    data class Located(val unit: ParagraphUnit, val start: Int, val end: Int)

    fun load(novelDir: File): Pair<List<ParagraphUnit>, List<Gold>> {
        val (located, gold) = locate(novelDir)
        return located.map { it.unit } to gold
    }

    /**
     * The same corpus with paragraph offsets kept, which is what byte spans need.
     *
     * Line breaks inside a paragraph become single spaces rather than being collapsed away:
     * one character in, one character out, so an offset into the file is still an offset
     * into the paragraph. Collapsing runs of whitespace — which this did before — shifts
     * every offset after the first indented line. Nothing downstream minds the extra spaces:
     * `Roster` and `Names` match on `\s+` throughout and normalise names before storing them.
     */
    fun locate(novelDir: File): Pair<List<Located>, List<Gold>> {
        val text = File(novelDir, "novel_text.txt").readText()
        val flat = text.map { if (it == '\n' || it == '\r' || it == '\t') ' ' else it }.joinToString("")

        val paragraphs = mutableListOf<Located>()
        val breaks = Regex("\n[ \t]*\n")
        var start = 0
        var n = 0
        while (start <= text.length) {
            val match = breaks.find(text, start)
            val end = match?.range?.first ?: text.length
            if (end > start && flat.substring(start, end).isNotBlank()) {
                paragraphs += Located(
                    ParagraphUnit("${novelDir.name}#p$n", flat.substring(start, end), 0, n),
                    start, end,
                )
                n++
            }
            if (match == null) break
            start = match.range.last + 1
        }

        val offsets = ByteOffsets(text)
        val gold = Csv.parse(File(novelDir, "quotation_info.csv").readText()).mapNotNull { row ->
            val quote = row["quoteText"]?.trim().orEmpty()
            val speaker = row["speaker"]?.trim().orEmpty()
            if (quote.isEmpty() || speaker.isEmpty()) null
            else Gold(
                id = row["quoteID"]?.trim().orEmpty(),
                text = quote.replace('\n', ' '),
                speaker = speaker,
                type = row["quoteType"]?.trim().orEmpty().let {
                    if (it.isEmpty() || it == "nan") "Unspecified" else it
                },
                spans = spans(row["quoteByteSpans"].orEmpty()).map {
                    offsets.charOf(it.first) to offsets.charOf(it.second)
                },
            )
        }
        return paragraphs to gold
    }

    /** Which paragraph a quotation opens in, and where within it. Null when it lands nowhere. */
    fun locateIn(paragraphs: List<Located>, gold: Gold): Triple<Int, Int, Int>? {
        val (from, to) = gold.spans.firstOrNull() ?: return null
        val paragraph = paragraphs.lastOrNull { it.start <= from } ?: return null
        if (from >= paragraph.end) return null
        return Triple(paragraph.unit.index, from - paragraph.start, minOf(to, paragraph.end) - paragraph.start)
    }

    data class Tally(var scored: Int = 0, var attributed: Int = 0, var correct: Int = 0) {
        val coverage get() = if (scored == 0) 0.0 else attributed * 100.0 / scored
        val precision get() = if (attributed == 0) 0.0 else correct * 100.0 / attributed
        val accuracy get() = if (scored == 0) 0.0 else correct * 100.0 / scored

        operator fun plusAssign(other: Tally) {
            scored += other.scored; attributed += other.attributed; correct += other.correct
        }
    }

    /**
     * Tier 1's tallies by quotation type, for the `pdnc` command.
     *
     * The scoring itself lives in [quire.spike.bakeoff.Bakeoff], which asks every candidate
     * the same questions. One aligner in the repository rather than two is the point: the
     * two that existed disagreed by a factor of thirteen on the denominator.
     */
    fun score(novelDir: File): Map<String, Tally> =
        quire.spike.bakeoff.Bakeoff.score(
            novelDir,
            quire.spike.bakeoff.Tier1Candidate(actionBeats = Tier1.useActionBeats),
        ).byType

    /** `[[2309, 2585], [2600, 2610]]` — PDNC writes these as Python literals. */
    fun spans(field: String): List<Pair<Int, Int>> =
        Regex("\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*]").findAll(field).map {
            it.groupValues[1].toInt() to it.groupValues[2].toInt()
        }.toList()

    /**
     * Byte offset to character offset.
     *
     * Every novel in the current revision is pure ASCII, so this is the identity today. It
     * exists because a corpus revision adding one accented name would otherwise shift every
     * span after it by a character, and be very hard to see in the numbers.
     */
    class ByteOffsets(text: String) {
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

    /**
     * PDNC names characters canonically ("Mr Elton"); our roster holds whatever appeared in
     * the tag ("Elton"). Count a match when either name's words contain the other's — alias
     * resolution proper is QUI-007's job.
     *
     * Honorifics are kept on purpose: folding them away would make "Mr Bennet" and
     * "Mrs Bennet" one person, and Austen is a fifth of this corpus.
     */
    fun matches(predicted: String, gold: String): Boolean {
        val p = words(predicted)
        val g = words(gold)
        if (p.isEmpty() || g.isEmpty()) return false
        return p == g || p.containsAll(g) || g.containsAll(p)
    }

    /**
     * Both sides are folded the same way, which they were not until 2026-09-02.
     *
     * Punctuation used to be stripped from the gold name and left on ours, so predicted
     * `Mr. Woodhouse` did not match gold `Mr. Woodhouse`: the words were `mr.` and `mr`.
     * Every honorific carrying a full stop scored as a miss, which is most of Austen, and
     * the bug deflated every PDNC number this repository has printed.
     */
    private fun words(name: String) = name.lowercase()
        .replace(Regex("[^a-z ]"), " ")
        .split(' ')
        .filter { it.isNotBlank() }
        .toSet()

    // ---- cast discovery (QUI-032) ------------------------------------------------

    /** One PDNC character: every name the novel calls them by, and their gender. */
    data class GoldCharacter(
        val mainName: String,
        val aliases: Set<String>,
        val gender: String,
        val category: String,
    )

    data class CastScore(
        val novel: String,
        val found: Int,
        val real: Int,
        val junk: List<String>,
        val expected: Int,
        val recalled: Int,
        val genderScored: Int,
        val genderRight: Int,
        val genderMissing: Int,
    ) {
        /** Share of real characters the scan dared assign a gender to at all. */
        val genderCoverage get() =
            if (real == 0) 0.0 else (real - genderMissing) * 100.0 / real
        val precision get() = if (found == 0) 0.0 else real * 100.0 / found
        val recall get() = if (expected == 0) 0.0 else recalled * 100.0 / expected
        val genderAccuracy get() = if (genderScored == 0) 0.0 else genderRight * 100.0 / genderScored
    }

    fun characters(novelDir: File): List<GoldCharacter> =
        Csv.parse(File(novelDir, "character_info.csv").readText()).mapNotNull { row ->
            val main = row["Main Name"]?.trim().orEmpty()
            if (main.isEmpty()) return@mapNotNull null
            // The Aliases column holds a Python set or list literal.
            val aliases = Regex("'([^']*)'").findAll(row["Aliases"].orEmpty())
                .map { it.groupValues[1] }.filter { it.isNotBlank() }.toSet()
            GoldCharacter(
                mainName = main,
                aliases = aliases + main,
                gender = row["Gender"]?.trim().orEmpty(),
                category = row["Category"]?.trim().orEmpty(),
            )
        }

    /**
     * Score the roster itself, rather than what it attributes.
     *
     * A cast is the first thing a reader sees after an import, and a wrong one is visible
     * in a way a wrong attribution is not: the app said the book has 157 people in it.
     * Recall is measured against the characters PDNC calls major or intermediate — a
     * one-line footman does not need his own voice, and counting him as a miss would
     * flatter nothing.
     */
    fun cast(novelDir: File): CastScore {
        val (paragraphs, _) = load(novelDir)
        val gold = characters(novelDir)
        val scan = Roster.scan(paragraphs.map { it.locator to it.text })
        val manifest = Roster.manifest(scan, novelDir.name, 0L)

        val junk = mutableListOf<String>()
        var real = 0
        var genderScored = 0
        var genderRight = 0
        var genderMissing = 0
        val hit = mutableSetOf<String>()
        for (character in manifest.characters) {
            val match = gold.firstOrNull { g -> g.aliases.any { matches(character.id, it) } }
            if (match == null) { junk += character.id; continue }
            real++
            hit += match.mainName
            val expected = when (match.gender) {
                "M" -> Gender.MALE
                "F" -> Gender.FEMALE
                else -> null
            }
            if (character.gender == Gender.UNKNOWN) genderMissing++
            if (expected != null && character.gender != Gender.UNKNOWN) {
                genderScored++
                if (character.gender == expected) genderRight++
            }
        }

        val wanted = gold.filter { it.category == "major" || it.category == "intermediate" }
        return CastScore(
            novel = novelDir.name,
            found = manifest.characters.size,
            real = real,
            junk = junk,
            expected = wanted.size,
            recalled = wanted.count { it.mainName in hit },
            genderScored = genderScored,
            genderRight = genderRight,
            genderMissing = genderMissing,
        )
    }
}
