package quire.spike.bakeoff

import java.io.File
import quire.attribution.scenes.SceneSegmenter
import quire.attribution.scenes.SceneSplitter
import quire.spike.Pdnc

/**
 * The `bakeoff` command (QUI-028): score a candidate across the whole corpus, with the
 * out-of-domain holdouts reported apart from the headline.
 *
 * Distinct from the `pdnc` command, which scores Tier 1 over the novel directories you name
 * and nothing else. This one needs the corpus root, because the holdout split is chosen from
 * PDNC's own novel index.
 */
object BakeoffCli {

    fun candidate(id: String, answers: File? = null): Candidate? = when {
        id == "tier1" -> Tier1Candidate()
        id == "tier1-nobeats" -> Tier1Candidate(actionBeats = false)
        id == "tier1-nopronouns" -> Tier1Candidate(pronouns = false)
        id == "tier1-tags-only" -> Tier1Candidate(pronouns = false, actionBeats = false)
        // The turn-taking rule ADR-0006 assumes and QUI-009 has not written. Scored beside the
        // models because untagged dialogue is what they were brought in for.
        id == "alternation" -> AlternationCandidate()
        id == "alternation-pairs" -> AlternationCandidate(pairsOnly = true)
        id == "alternation-adjacent" -> AlternationCandidate(adjacentOnly = true)
        id == "alternation-adjacent-pairs" ->
            AlternationCandidate(pairsOnly = true, adjacentOnly = true)
        id == "alternation-tags-only" ->
            AlternationCandidate(Tier1Candidate(pronouns = false, actionBeats = false))
        // Anything a predictor outside this JVM produced — see ExternalCandidate. The id is
        // kept as given so the report names the model rather than the mechanism.
        answers != null -> ExternalCandidate(answers, id)
        else -> null
    }

    /**
     * Write what a predictor needs and nothing more (QUI-028).
     *
     * Paragraph text and quotation offsets go out; **gold speakers do not**. A predictor that
     * cannot see the answer cannot accidentally score itself, and the scoring stays on this
     * side for every candidate alike.
     */
    fun dump(root: File, out: File, only: Set<String>) {
        val index = Pdnc.index(root).filter { only.isEmpty() || it.folder in only }
        out.mkdirs()
        println("Dumping ${index.size} novels to ${out.path}")
        var quotations = 0
        for (meta in index) {
            val (paragraphs, gold) = Pdnc.locate(File(File(root, "data"), meta.folder))
            val (questions, _) = Bakeoff.questions(paragraphs, gold)
            File(out, "${meta.folder}.paragraphs.jsonl").printWriter().use { w ->
                paragraphs.forEach { p ->
                    w.println("""{"n":${p.unit.index},"text":${jsonString(p.unit.text)}}""")
                }
            }
            // Scenes come from the real segmenter (QUI-038), not from anything the
            // predictor reinvents — a predictor that split scenes even slightly differently
            // would be scored against questions it never saw in context.
            val prepared = SceneReport.reconstructChapters(paragraphs.map { it.unit })
            val tokens = prepared.associate { it.index to SceneReport.estimateTokens(it.text) }
            File(out, "${meta.folder}.scenes.jsonl").printWriter().use { w ->
                for (scene in SceneSegmenter.segment(prepared)) {
                    val pieces = SceneSplitter.split(
                        scene, prepared, SceneReport.BUDGET, { tokens.getValue(it.index) })
                    for (piece in pieces) {
                        w.println(
                            """{"start":${piece.range.start},"endExclusive":${piece.range.endExclusive},""" +
                                """"atTurnBoundary":${piece.atTurnBoundary}}"""
                        )
                    }
                }
            }
            File(out, "${meta.folder}.questions.jsonl").printWriter().use { w ->
                questions.forEach { q ->
                    w.println(
                        """{"id":${jsonString(q.id)},"paragraph":${q.paragraph},""" +
                            """"start":${q.start},"end":${q.end},"type":${jsonString(q.type)}}"""
                    )
                }
            }
            quotations += questions.size
            println("  %-32s %5d paragraphs  %5d quotations".format(meta.folder, paragraphs.size, questions.size))
        }
        println("\n$quotations quotations. A predictor writes <novel>.answers.tsv beside these:")
        println("  id<TAB>speaker<TAB>evidence — an empty speaker means it declined.")
        println("Then: bakeoff --candidate <name> --answers ${out.path}")
    }

    fun root(flag: String?): File? {
        val path = flag
            ?: System.getenv("PDNC_HOME")
            ?: (System.getProperty("user.home") + "/.cache/quire/pdnc")
        val root = File(path)
        return if (File(root, "PDNC-Novel-Index.csv").exists()) root else null
    }

    fun run(root: File, candidate: Candidate, only: Set<String>, perNovel: Boolean, showMistakes: Boolean) {
        val index = Pdnc.index(root).filter { only.isEmpty() || it.folder in only }

        println("Attribution bake-off — QUI-028")
        println("corpus:    ${root.path} (${index.size} novels)")
        println("candidate: ${candidate.id} — ${candidate.description}")
        println()

        val scores = index.map { meta ->
            Bakeoff.score(File(File(root, "data"), meta.folder), candidate, meta)
        }
        val headline = scores.filterNot { Holdouts.heldOut(it.meta) }
        val held = scores.filter { Holdouts.heldOut(it.meta) }

        Report.table("PDNC headline — ${headline.size} novels, held-out books excluded", headline)
        if (held.isNotEmpty()) {
            Report.holdouts(held)
            Report.gap(headline, held)
        }
        Report.table("Whole corpus, for comparison with published figures", scores)
        if (perNovel) Report.perNovel("Per novel", scores)
        Report.evidence(scores)
        if (showMistakes) Report.mistakes(scores)

        println("Diagnostics")
        println("  quotations skipped, speaker is a PDNC pseudo-entity   ${scores.sumOf { it.unscorable }}")
        println("  quotations whose byte span landed outside a paragraph ${scores.sumOf { it.unlocatable }}")
        println()
        println("Published comparisons (docs/prior-art.md §3): BookNLP ~63%, encoder with joint")
        println("scoring 94.5%. Both are whole-corpus figures, so compare them against the")
        println("whole-corpus row and not against the headline split.")
    }

    fun holdouts(root: File) {
        val index = Pdnc.index(root)
        println("Out-of-domain split — QUI-028\n")
        for ((axis, members) in Holdouts.byAxis(index)) {
            println("${axis.id}: ${axis.asks}")
            if (members.isEmpty()) println("  (nothing in PDNC qualifies)")
            for (m in members) {
                println("  %-30s %s, %s, %s person, %d".format(
                    m.folder, m.genre, if (m.translated) "translated" else "English original",
                    if (m.narrativePerson == 1) "first" else "third", m.year))
            }
            println()
        }
        val headline = Holdouts.headline(index)
        println("headline set: ${headline.size} novels; held out: ${index.size - headline.size}")
        println()
        println("Every one of these is still a PDNC novel. The split shows a candidate tuned to")
        println("the corpus's centre of mass; it cannot show degradation on contemporary prose,")
        println("because PDNC stops in 1934. See Holdouts for the slot that would.")
    }

    fun scenes(root: File, only: Set<String>) = SceneReport.run(root, only)

    fun novels(root: File) {
        println("%-30s %-26s %-10s %-12s %6s %s".format("folder", "title", "person", "genre", "year", "held out"))
        for (m in Pdnc.index(root)) {
            println("%-30s %-26s %-10s %-12s %6d %s".format(
                m.folder, m.title.take(26), if (m.narrativePerson == 1) "first" else "third",
                m.genre + if (m.translated) "*" else "", m.year,
                Holdouts.axisOf(m).joinToString(",") { it.id }.ifEmpty { "—" }))
        }
        println("\n* translated")
    }
}
