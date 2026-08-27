package quire.spike

import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """
quire-pipeline-spike (QUI-018)

  score <fixture.tsv>...      Tier 1 coverage, precision and accuracy on labelled fixtures
  transcript <fixture.tsv>    per-span decisions with the evidence for each
  roster <fixture.tsv>...     the roster bootstrapped from speech tags, with no model
  epub <book.epub>            attribute a real book and print the same transcript

Tier 2/3 are not implemented: they need the runtime chosen by ADR-0001 (QUI-017).
"""

fun main(args: Array<String>) {
    if (args.isEmpty()) { println(USAGE.trim()); exitProcess(2) }
    val files = args.drop(1).map(::File)
    val missing = files.filterNot { it.isFile }
    if (missing.isNotEmpty()) {
        System.err.println("not found: ${missing.joinToString { it.path }}")
        exitProcess(2)
    }
    when (args[0]) {
        "score" -> score(files)
        "transcript" -> transcript(files.first())
        "roster" -> roster(files)
        "epub" -> epub(files.first())
        else -> { println(USAGE.trim()); exitProcess(2) }
    }
}

private fun pct(d: Double) = "%.1f%%".format(d * 100)

private fun score(files: List<File>) {
    val overall = Score()
    println("Tier 1 heuristic attribution — QUI-008\n")
    for (f in files) {
        val s = Scorer.score(Fixture.load(f))
        println("${f.name}")
        println("  spans scored   ${s.scored}")
        println("  coverage       ${pct(s.coverage)}  (${s.attributed}/${s.scored} attributed by Tier 1)")
        println("  precision      ${pct(s.precision)}  (${s.correct}/${s.attributed} of those correct)")
        println("  accuracy       ${pct(s.accuracy)}  (${s.correct}/${s.scored} of all spans)")
        s.mistakes.forEach { println("  MISTAKE $it") }
        println()
        overall.scored += s.scored; overall.attributed += s.attributed; overall.correct += s.correct
        s.byEvidence.forEach { (k, v) ->
            val b = overall.byEvidence.getOrPut(k) { IntArray(2) }; b[0] += v[0]; b[1] += v[1]
        }
    }
    println("TOTAL")
    println("  spans scored   ${overall.scored}")
    println("  coverage       ${pct(overall.coverage)}")
    println("  precision      ${pct(overall.precision)}")
    println("  accuracy       ${pct(overall.accuracy)}")
    println("\n  by evidence:")
    overall.byEvidence.forEach { (evidence, b) ->
        println("    %-22s %3d attributed, %3d correct".format(evidence, b[0], b[1]))
    }
    println("\n  Spans left for Tier 2/3: ${overall.scored - overall.attributed} " +
        "(${pct(1 - overall.coverage)} of dialogue). On the reference device that is what the")
    println("  SLM has to process — see docs/device-profile.md §2.")
}

private fun transcript(file: File) {
    val labelled = Fixture.load(file)
    val units = labelled.map { it.unit }
    val roster = Tier1.bootstrapRoster(units).names
    println("roster (bootstrapped, no model): ${roster.sorted().joinToString(", ")}\n")
    for (r in Tier1.attribute(units, roster)) {
        val who = r.speakerId ?: if (r.kind == Kind.NARRATION) "—" else "???"
        println("%-10s %-4s %.2f  %-22s %s".format(
            who, r.tier.name.take(4), r.confidence, r.evidence, r.text.take(64)))
    }
}

private fun epub(file: File) {
    val units = Epub.paragraphs(file)
    val roster = Tier1.bootstrapRoster(units)
    println("${units.size} paragraphs, ${units.map { it.chapterIndex }.distinct().size} spine items")
    println("roster from speech tags: ${roster.fromTags.keys.sorted().joinToString(", ")}")
    println("roster from adjacency:   ${roster.fromAdjacency.filterValues { it >= Tier1.ADJACENCY_MIN }.keys.sorted().joinToString(", ")}")
    val results = Tier1.attribute(units, roster.names)
    val dialogue = results.filter { it.kind == Kind.DIALOGUE }
    val attributed = dialogue.count { it.speakerId != null }
    println("dialogue spans: ${dialogue.size}, Tier 1 attributed: $attributed " +
        "(%.1f%%), left for Tier 2/3: ${dialogue.size - attributed}\n".format(
            if (dialogue.isEmpty()) 0.0 else attributed * 100.0 / dialogue.size))
    for (r in results) {
        val who = r.speakerId ?: if (r.kind == Kind.NARRATION) "—" else "???"
        println("%-14s %-4s %.2f  %-22s %s".format(
            who, r.tier.name.take(4), r.confidence, r.evidence, r.text.take(60)))
    }
}

private fun roster(files: List<File>) {
    for (f in files) {
        val r = Tier1.bootstrapRoster(Fixture.load(f).map { it.unit })
        fun show(m: Map<String, Int>) = m.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.key} (${it.value})" }.ifEmpty { "—" }
        println("${f.name}")
        println("  from speech tags: ${show(r.fromTags)}")
        println("  from adjacency:   ${show(r.fromAdjacency)}  [admitted at >= ${Tier1.ADJACENCY_MIN}]")
        println("  roster:           ${r.names.sorted().joinToString(", ")}")
    }
}
