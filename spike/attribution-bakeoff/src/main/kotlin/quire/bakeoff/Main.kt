package quire.bakeoff

import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """
quire-attribution-bakeoff (QUI-028)

  score [--corpus DIR] [--candidate ID] [--novels A,B] [--per-novel] [--mistakes]
                          score a candidate on PDNC: headline, holdouts and the gap
  holdouts [--corpus DIR] the out-of-domain split and why each novel is in it
  novels   [--corpus DIR] what PDNC holds, from its own index

Candidates: tier1, tier1-nobeats, tier1-nopronouns, tier1-tags-only
The encoder and SLM candidates are not implemented — see the ticket worklog for the
model file the encoder needs and why this container cannot fetch it.

PDNC is not committed. Fetch it with tools/fetch-pdnc.sh; --corpus defaults to
${'$'}PDNC_HOME, then ~/.cache/quire/pdnc.
"""

private fun corpusRoot(flags: Map<String, String>): File {
    val path = flags["corpus"]
        ?: System.getenv("PDNC_HOME")
        ?: (System.getProperty("user.home") + "/.cache/quire/pdnc")
    val root = File(path)
    if (!File(root, "PDNC-Novel-Index.csv").exists()) {
        System.err.println("no PDNC at ${root.path} — run tools/fetch-pdnc.sh first")
        exitProcess(2)
    }
    return root
}

private fun candidate(id: String): Candidate = when (id) {
    "tier1" -> Tier1Candidate()
    "tier1-nobeats" -> Tier1Candidate(actionBeats = false)
    "tier1-nopronouns" -> Tier1Candidate(pronouns = false)
    "tier1-tags-only" -> Tier1Candidate(pronouns = false, actionBeats = false)
    else -> {
        System.err.println("unknown candidate: $id")
        exitProcess(2)
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) { println(USAGE.trim()); exitProcess(2) }
    val flags = parseFlags(args.drop(1))

    when (args[0]) {
        "score" -> score(flags)
        "holdouts" -> holdouts(flags)
        "novels" -> novels(flags)
        else -> { println(USAGE.trim()); exitProcess(2) }
    }
}

/** `--key value`, `--key=value` and bare `--key` all mean the same thing to a shell user. */
internal fun parseFlags(args: List<String>): Map<String, String> {
    val out = linkedMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        if (!arg.startsWith("--")) { i++; continue }
        val body = arg.removePrefix("--")
        val eq = body.indexOf('=')
        if (eq >= 0) {
            out[body.substring(0, eq)] = body.substring(eq + 1)
        } else {
            val next = args.getOrNull(i + 1)
            if (next != null && !next.startsWith("--")) { out[body] = next; i++ } else out[body] = ""
        }
        i++
    }
    return out
}

private fun score(flags: Map<String, String>) {
    val root = corpusRoot(flags)
    val candidate = candidate(flags["candidate"].orEmpty().ifEmpty { "tier1" })
    val wanted = flags["novels"].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val index = Corpus.index(root).filter { wanted.isEmpty() || it.folder in wanted }

    println("Attribution bake-off — QUI-028")
    println("corpus:    ${root.path} (${index.size} novels)")
    println("candidate: ${candidate.id} — ${candidate.description}")
    println()

    val scores = mutableListOf<Scorer.NovelScore>()
    var unscorable = 0
    var unlocatable = 0
    for (meta in index) {
        val novel = Corpus.load(root, meta)
        unscorable += novel.unscorable
        unlocatable += novel.unlocatable
        scores += Scorer.score(novel, candidate)
    }

    val headline = scores.filterNot { Holdouts.heldOut(it.meta) }
    val held = scores.filter { Holdouts.heldOut(it.meta) }

    Report.table("PDNC headline — ${headline.size} novels, held-out books excluded", headline)
    if (held.isNotEmpty()) {
        Report.holdouts(held)
        Report.gap(headline, held)
    }
    Report.table("Whole corpus, for comparison with published figures", scores)
    if ("per-novel" in flags) Report.perNovel("Per novel", scores)
    Report.evidence(scores)
    if ("mistakes" in flags) Report.mistakes(scores)

    println("Diagnostics")
    println("  quotations skipped, speaker is a PDNC pseudo-entity  $unscorable")
    println("  quotations whose byte span landed outside a paragraph $unlocatable")
    println()
    println("Published comparisons (docs/prior-art.md §3): BookNLP ~63%, encoder with joint")
    println("scoring 94.5%. Both are whole-corpus accuracy figures, so compare them against")
    println("the whole-corpus row and not against the headline split.")
}

private fun holdouts(flags: Map<String, String>) {
    val index = Corpus.index(corpusRoot(flags))
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

private fun novels(flags: Map<String, String>) {
    val index = Corpus.index(corpusRoot(flags))
    println("%-30s %-26s %-10s %-12s %6s %s".format("folder", "title", "person", "genre", "year", "held out"))
    for (m in index) {
        println("%-30s %-26s %-10s %-12s %6d %s".format(
            m.folder, m.title.take(26), if (m.narrativePerson == 1) "first" else "third",
            m.genre + if (m.translated) "*" else "", m.year,
            Holdouts.axisOf(m).joinToString(",") { it.id }.ifEmpty { "—" }))
    }
    println("\n* translated")
}
