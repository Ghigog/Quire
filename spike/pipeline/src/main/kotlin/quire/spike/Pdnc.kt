package quire.spike

import java.io.File

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
}
