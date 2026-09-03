package quire.spike.bakeoff

import quire.spike.Pdnc
import quire.spike.Pdnc.Tally
import quire.spike.bakeoff.Bakeoff.NovelScore

/** Prints the tables the ADR will quote. Nothing here decides anything. */
object Report {

    private val TYPES = listOf("Explicit", "Implicit", "Anaphoric", "Unspecified")

    private fun row(label: String, t: Tally) = "%-34s %7d %9.1f%% %9.1f%% %9.1f%%".format(
        label.take(34), t.scored, t.coverage, t.precision, t.accuracy)

    private const val HEAD = "%-34s %7s %10s %10s %10s"

    fun table(title: String, scores: List<NovelScore>) {
        println(title)
        println(HEAD.format("", "quotes", "coverage", "precision", "accuracy"))
        val byType = linkedMapOf<String, Tally>()
        for (s in scores) for ((type, t) in s.byType) byType.getOrPut(type) { Tally() } += t
        for (type in TYPES) byType[type]?.let { println(row("  $type", it)) }
        byType.keys.filterNot { it in TYPES }.forEach { println(row("  $it", byType.getValue(it))) }
        println(row("  all", total(scores)))
        println()
    }

    fun total(scores: List<NovelScore>) = Tally().also { t -> scores.forEach { t += it.total } }

    fun perNovel(title: String, scores: List<NovelScore>) {
        println(title)
        println(HEAD.format("", "quotes", "coverage", "precision", "accuracy") + "  explicit  held out")
        for (s in scores.sortedByDescending { it.total.accuracy }) {
            val explicit = s.byType["Explicit"]?.scored ?: 0
            val share = if (s.total.scored == 0) 0.0 else explicit * 100.0 / s.total.scored
            val axes = Holdouts.axisOf(s.meta).joinToString(",") { it.id }
            println(row("  ${s.meta.folder}", s.total) + "   %6.1f%%  %s".format(share, axes.ifEmpty { "—" }))
        }
        println()
    }

    fun holdouts(scores: List<NovelScore>) {
        println("Out-of-domain holdouts — scored apart from the headline (QUI-028)")
        println(HEAD.format("", "quotes", "coverage", "precision", "accuracy"))
        for (axis in Holdouts.Axis.entries) {
            val members = scores.filter { axis in Holdouts.axisOf(it.meta) }
            if (members.isEmpty()) { println("  %-32s (no novel in the corpus)".format(axis.id)); continue }
            println(row("  ${axis.id}", total(members)))
            println("      %s: %s".format(axis.asks, members.joinToString(", ") { it.meta.folder }))
        }
        println(row("  all holdouts", total(scores)))
        println()
    }

    fun gap(headline: List<NovelScore>, held: List<NovelScore>) {
        val a = total(headline)
        val b = total(held)
        println("Expected real-library degradation")
        println("  PDNC headline accuracy   %.1f%%  (%d quotations, %d novels)".format(a.accuracy, a.scored, headline.size))
        println("  Held-out accuracy        %.1f%%  (%d quotations, %d novels)".format(b.accuracy, b.scored, held.size))
        println("  Gap                      %+.1f points".format(b.accuracy - a.accuracy))
        println()
        println("  Read that as a LOWER bound. The holdouts are PDNC novels chosen off one axis")
        println("  each; the corpus stops in 1934, so nothing here measures contemporary prose.")
        println("  Holdouts.External is the slot that would, and it is empty.")
        println()
    }

    fun evidence(scores: List<NovelScore>) {
        val byEvidence = linkedMapOf<String, Tally>()
        for (s in scores) for ((e, t) in s.byEvidence) byEvidence.getOrPut(e) { Tally() } += t
        println("By evidence — what each rule was asked and what it got right")
        println("%-34s %7s %10s %10s %10s".format("", "asked", "answered", "precision", "share"))
        val asked = byEvidence.values.sumOf { it.scored }.coerceAtLeast(1)
        for ((e, t) in byEvidence.entries.sortedByDescending { it.value.scored }) {
            println("  %-32s %7d %9d %9.1f%% %9.1f%%".format(
                e.take(32), t.scored, t.attributed, t.precision, t.scored * 100.0 / asked))
        }
        println()
    }

    fun mistakes(scores: List<NovelScore>, limit: Int = 20) {
        val all = scores.flatMap { it.mistakes }.take(limit)
        if (all.isEmpty()) return
        println("A sample of wrong answers")
        for (m in all) {
            println("  %-28s %-11s gold %-20s said %-20s %s".format(
                m.questionId.take(28), m.type, m.gold.take(20), m.predicted.take(20), m.quote))
        }
        println()
    }
}
