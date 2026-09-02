package quire.spike

import java.io.File
import quire.attribution.Roster
import quire.epub.EpubText
import kotlin.system.exitProcess

private const val USAGE = """
quire-pipeline-spike (QUI-018)

  score <fixture.tsv>...      Tier 1 coverage, precision and accuracy on labelled fixtures
  transcript <fixture.tsv>    per-span decisions with the evidence for each
  roster <fixture.tsv>...     the roster bootstrapped from speech tags, with no model
  epub <book.epub>            attribute a real book and print the same transcript
  pdnc <pdnc/data/Novel>...   score Tier 1 against PDNC, split by quotation type
                              add --no-beats to disable the action-beat rule
  cast <pdnc/data/Novel>...   score the roster itself: how much of the cast it finds,
                              how much of it is junk, and whether the genders are right
                              (gknown: the share it dared assign a gender at all)
                              add --junk to list the invented characters
  export <book.epub> <out.tsv>  attribute a real book and write the segments for
                              spike/indexer to turn into a dialogue index

Tier 2/3 are not implemented: they need the runtime chosen by ADR-0001 (QUI-017).
"""

fun main(args: Array<String>) {
    if (args.isEmpty()) { println(USAGE.trim()); exitProcess(2) }
    val flags = args.drop(1).filter { it.startsWith("--") }
    Tier1.useActionBeats = "--no-beats" !in flags
    val files = args.drop(1).filterNot { it.startsWith("--") }.map(::File)
    // `export` names a file it is about to create, so only its input has to exist.
    val inputs = if (args[0] == "export") files.take(1) else files
    val missing = inputs.filterNot { it.exists() } // pdnc takes directories
    if (missing.isNotEmpty()) {
        System.err.println("not found: ${missing.joinToString { it.path }}")
        exitProcess(2)
    }
    when (args[0]) {
        "score" -> score(files)
        "transcript" -> transcript(files.first())
        "roster" -> roster(files)
        "epub" -> epub(files.first())
        "pdnc" -> pdnc(files)
        "cast" -> cast(files, verbose = "--junk" in flags)
        "export" -> export(files[0], files.getOrElse(1) { File("attributed.tsv") })
        else -> { println(USAGE.trim()); exitProcess(2) }
    }
}

/**
 * Attribute a real book and write the result where the indexer can read it.
 *
 * A TSV rather than a direct module dependency, for two reasons. The pipeline and the
 * indexer stay independent, and — the one that matters — the intermediate is *inspectable*:
 * when a character speaks in the wrong voice on device, the question is always whether
 * attribution was wrong or the matcher was, and a file you can open answers it in seconds.
 *
 * Every segment is written, narration included, so concatenating the text column
 * reconstructs the book. The indexer relies on that to rebuild sentences.
 */
private fun export(book: File, out: File) {
    val units = EpubText.paragraphs(book)
    val roster = Tier1.bootstrapRoster(units)
    // Tier 1, then turn-taking. Most dialogue carries no tag at all, and an exchange the
    // author tagged once is the common case rather than the exception.
    val results = quire.attribution.Conversation.resolve(
        Tier1.attribute(units, roster),
        cast = roster.names.toList(),
    )

    // Each paragraph is written before its own segments, with its text exactly as the
    // reader will see it. Segment text is trimmed, so rebuilding a paragraph by joining
    // segments would drift from what the host actually sends — and a paragraph that
    // differs by one space matches nothing.
    val bySegment = results.associateBy { it.locator }
    out.printWriter().use { w ->
        w.println("locator\tkind\tspeaker\tconfidence\ttier\ttext")
        for (unit in units) {
            val flat = unit.text.replace("\t", " ").replace("\n", " ")
            if (flat.isBlank()) continue
            w.println("${unit.locator}\tPARAGRAPH\t\t0.00\tNONE\t$flat")
            for (seg in Text.segment(unit)) {
                val r = bySegment[seg.locator] ?: continue
                val text = r.text.replace("\t", " ").replace("\n", " ")
                w.println("${r.locator}\t${r.kind}\t${r.speakerId ?: ""}\t" +
                    "%.2f\t${r.tier}\t$text".format(r.confidence))
            }
        }
    }

    // The cast travels with the attribution: genders are what let the service voice
    // Sarah as a woman, and re-deriving them downstream would be a second implementation
    // of the thing that decides how the book sounds.
    val manifest = File(out.parentFile ?: File("."), "characters.json")
    manifest.writeText(
        quire.model.characters.ManifestCodec.encode(
            Roster.manifest(roster, bookId = book.nameWithoutExtension, generatedAt = System.currentTimeMillis()),
        ),
    )

    val dialogue = results.filter { it.kind == Kind.DIALOGUE }
    val named = dialogue.count { it.speakerId != null }
    println("${units.size} paragraphs, ${results.size} segments -> ${out.path}")
    println("cast -> ${manifest.path}")
    println("roster: ${roster.names.sorted().joinToString(", ")}")
    println("dialogue spans %d, attributed %d (%s)".format(
        dialogue.size, named, pct(if (dialogue.isEmpty()) 0.0 else named.toDouble() / dialogue.size)))
    println()
    println("Tier 1 only. QUI-028 measured it at 58.5%% precision on PDNC, so expect a")
    println("meaningful share of these to be wrong -- which is what listening is for.")
}

/**
 * How good is the cast we show the reader?
 *
 * Attribution accuracy says nothing about this: a roster can be 90% junk and still get
 * every tagged line right, because the junk is never in tag position. It is the number the
 * reader actually sees, so it gets measured on real novels rather than on fixtures written
 * to exercise the rules.
 */
private fun cast(dirs: List<File>, verbose: Boolean) {
    println("Cast discovery vs PDNC character_info.csv\n")
    println("%-28s %6s %6s %6s %6s %8s %8s".format(
        "novel", "found", "real", "prec", "recall", "gender", "gknown"))
    var found = 0; var real = 0; var expected = 0; var recalled = 0
    var genderScored = 0; var genderRight = 0; var genderMissing = 0
    for (dir in dirs) {
        val s = Pdnc.cast(dir)
        found += s.found; real += s.real; expected += s.expected; recalled += s.recalled
        genderScored += s.genderScored; genderRight += s.genderRight
        genderMissing += s.genderMissing
        println("%-28s %6d %6d %5.1f%% %5.1f%% %7.1f%% %7.1f%%".format(
            s.novel.take(28), s.found, s.real, s.precision, s.recall,
            s.genderAccuracy, s.genderCoverage))
        if (verbose && s.junk.isNotEmpty()) println("    junk: " + s.junk.joinToString(", "))
    }
    if (dirs.size > 1) {
        println("%-28s %6d %6d %5.1f%% %5.1f%% %7.1f%% %7.1f%%".format(
            "ALL", found, real,
            if (found == 0) 0.0 else real * 100.0 / found,
            if (expected == 0) 0.0 else recalled * 100.0 / expected,
            if (genderScored == 0) 0.0 else genderRight * 100.0 / genderScored,
            if (real == 0) 0.0 else (real - genderMissing) * 100.0 / real))
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
    val cast = Tier1.bootstrapRoster(units)
    println("roster (bootstrapped, no model): ${cast.names.sorted().joinToString(", ")}\n")
    for (r in Tier1.attribute(units, cast)) {
        val who = r.speakerId ?: if (r.kind == Kind.NARRATION) "—" else "???"
        println("%-10s %-4s %.2f  %-22s %s".format(
            who, r.tier.name.take(4), r.confidence, r.evidence, r.text.take(64)))
    }
}

private fun epub(file: File) {
    val units = EpubText.paragraphs(file)
    val roster = Tier1.bootstrapRoster(units)
    println("${units.size} paragraphs, ${units.map { it.chapterIndex }.distinct().size} spine items")
    println("roster from speech tags: ${roster.fromTags.keys.sorted().joinToString(", ")}")
    println("roster from adjacency:   ${roster.fromAdjacency.filterValues { it >= Tier1.ADJACENCY_MIN }.keys.sorted().joinToString(", ")}")
    val results = Tier1.attribute(units, roster)
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

private fun pdnc(dirs: List<File>) {
    println("Tier 1 against PDNC — QUI-028\n")
    println("%-30s %-10s %6s %9s %10s %9s".format("novel", "type", "quotes", "coverage", "precision", "accuracy"))
    val overall = linkedMapOf<String, Pdnc.Tally>()
    for (dir in dirs) {
        val tallies = Pdnc.score(dir)
        for ((type, t) in tallies.entries.sortedBy { it.key }) {
            println("%-30s %-10s %6d %8.1f%% %9.1f%% %8.1f%%".format(
                dir.name.take(30), type, t.scored, t.coverage, t.precision, t.accuracy))
            val o = overall.getOrPut(type) { Pdnc.Tally() }
            o.scored += t.scored; o.attributed += t.attributed; o.correct += t.correct
        }
    }
    println()
    val all = Pdnc.Tally()
    for ((type, t) in overall.entries.sortedBy { it.key }) {
        println("%-30s %-10s %6d %8.1f%% %9.1f%% %8.1f%%".format("TOTAL", type, t.scored, t.coverage, t.precision, t.accuracy))
        all.scored += t.scored; all.attributed += t.attributed; all.correct += t.correct
    }
    println("%-30s %-10s %6d %8.1f%% %9.1f%% %8.1f%%".format("TOTAL", "all", all.scored, all.coverage, all.precision, all.accuracy))
    println("\nCoverage is the share of matched quotations Tier 1 attributed at all;")
    println("precision, of those, how many named the right speaker.")
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
