package quire.spike.bakeoff

import java.io.File
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

    fun candidate(id: String): Candidate? = when (id) {
        "tier1" -> Tier1Candidate()
        "tier1-nobeats" -> Tier1Candidate(actionBeats = false)
        "tier1-nopronouns" -> Tier1Candidate(pronouns = false)
        "tier1-tags-only" -> Tier1Candidate(pronouns = false, actionBeats = false)
        else -> null
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
