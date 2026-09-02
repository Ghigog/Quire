package quire.spike

import java.io.File
import quire.attribution.Roster
import quire.model.characters.Gender

/**
 * Scores Tier 1 against the Project Dialogism Novel Corpus (QUI-028, QUI-018).
 *
 * PDNC labels every quotation `Explicit` (a named speech tag), `Anaphoric` (a pronoun tag)
 * or `Implicit` (no tag). That split is exactly our tier boundary, so scoring against it
 * says not just how accurate Tier 1 is but how much of a real book it can *ever* reach.
 *
 * The corpus carries no licence and is not committed. Clone it separately; see
 * `docs/prior-art.md` §3.
 */
object Pdnc {

    data class Gold(val text: String, val speaker: String, val type: String)

    fun load(novelDir: File): Pair<List<ParagraphUnit>, List<Gold>> {
        val text = File(novelDir, "novel_text.txt").readText()
        // PDNC's text uses blank lines between paragraphs.
        val paragraphs = text.split(Regex("\n\\s*\n"))
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { i, body -> ParagraphUnit("${novelDir.name}#p$i", body, 0, i) }

        val gold = Csv.parse(File(novelDir, "quotation_info.csv").readText()).mapNotNull { row ->
            val quote = row["quoteText"]?.trim().orEmpty()
            val speaker = row["speaker"]?.trim().orEmpty()
            if (quote.isEmpty() || speaker.isEmpty()) null
            else Gold(quote, speaker, row["quoteType"]?.trim().orEmpty().ifEmpty { "Unknown" })
        }
        return paragraphs to gold
    }

    data class Tally(var scored: Int = 0, var attributed: Int = 0, var correct: Int = 0) {
        val coverage get() = if (scored == 0) 0.0 else attributed * 100.0 / scored
        val precision get() = if (attributed == 0) 0.0 else correct * 100.0 / attributed
        val accuracy get() = if (scored == 0) 0.0 else correct * 100.0 / scored
    }

    fun score(novelDir: File): Map<String, Tally> {
        val (paragraphs, gold) = load(novelDir)
        val roster = Tier1.bootstrapRoster(paragraphs)
        val predictions = Tier1.attribute(paragraphs, roster.names)
            .filter { it.kind == Kind.DIALOGUE }

        // Key predictions by their normalised text so a gold quotation can find ours
        // without depending on byte offsets surviving our whitespace collapsing.
        val byText = HashMap<String, MutableList<AttributionResult>>()
        for (p in predictions) byText.getOrPut(key(p.text)) { mutableListOf() } += p

        val tallies = linkedMapOf<String, Tally>()
        for (g in gold) {
            val tally = tallies.getOrPut(g.type) { Tally() }
            val ours = byText[key(g.text)]?.removeFirstOrNull() ?: continue
            tally.scored++
            val predicted = ours.speakerId ?: continue
            tally.attributed++
            if (matches(predicted, g.speaker)) tally.correct++
        }
        return tallies
    }

    /** Gold quotes carry their surrounding quote marks inconsistently; strip and fold. */
    private fun key(s: String) = s.trim().trim('"', '“', '”', '\'', ' ')
        .lowercase().replace(Regex("[^a-z0-9' ]"), "").replace(Regex("\\s+"), " ").trim()

    /**
     * PDNC names characters canonically ("Mr Elton"); our roster holds whatever appeared in
     * the tag ("Elton"). Count a match when either name's words contain the other's — alias
     * resolution proper is QUI-007's job.
     */
    private fun matches(predicted: String, gold: String): Boolean {
        val p = predicted.lowercase().split(' ').filter { it.isNotBlank() }.toSet()
        val g = gold.lowercase().replace(Regex("[^a-z ]"), "").split(' ')
            .filter { it.isNotBlank() }.toSet()
        if (p.isEmpty() || g.isEmpty()) return false
        return p == g || p.containsAll(g) || g.containsAll(p)
    }

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
