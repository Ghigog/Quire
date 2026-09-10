package quire.spike.bakeoff

import java.io.File
import quire.spike.Pdnc

/**
 * Does the prose obey the conventions [AlternationCandidate] reads? (QUI-028)
 *
 * The rules the candidate implements are published ones — CMOS and New Hart's Rules on
 * paragraphing and quotation, conversation analysis on turn-taking — so when the candidate is
 * wrong there are two possible culprits and they call for opposite work. Either we misread a
 * book that was set correctly, which is our bug; or the book departed from the convention,
 * which no rule can be fixed to catch and which puts a ceiling on the whole approach.
 *
 * This report separates them, using **gold speakers only**. No candidate runs, nothing is
 * attributed: it asks what the annotated truth does at each place a convention makes a claim.
 * A rule cannot score above the conformance of the text it is reading, so these are the
 * ceilings [AlternationCandidate] is working under, and the gap between a rule's precision and
 * its ceiling is the part that is ours to fix.
 */
object ConventionReport {

    /**
     * A turn as the conventions define one: a paragraph, and the speaker of everything quoted
     * inside it. [split] is true when the paragraph's quotations disagree about that, which is
     * itself a departure from the one-speaker-per-paragraph rule.
     */
    private data class Turn(val paragraph: Int, val speaker: String, val split: Boolean)

    private class Counts {
        var multiQuotationParagraphs = 0
        var splitParagraphs = 0
        var adjacentPairs = 0
        var adjacentSameSpeaker = 0
        var alternationChances = 0
        var alternationHolds = 0
        var openQuotations = 0
        var openQuotationsContinued = 0

        operator fun plusAssign(other: Counts) {
            multiQuotationParagraphs += other.multiQuotationParagraphs
            splitParagraphs += other.splitParagraphs
            adjacentPairs += other.adjacentPairs
            adjacentSameSpeaker += other.adjacentSameSpeaker
            alternationChances += other.alternationChances
            alternationHolds += other.alternationHolds
            openQuotations += other.openQuotations
            openQuotationsContinued += other.openQuotationsContinued
        }
    }

    fun run(root: File, only: Set<String>) {
        val index = Pdnc.index(root).filter { only.isEmpty() || it.folder in only }
        println("Convention conformance — QUI-028")
        println("corpus: ${root.path} (${index.size} novels), gold speakers only\n")

        val total = Counts()
        println("%-30s %10s %10s %10s".format("novel", "1/para", "break=turn", "alternates"))
        for (meta in index) {
            val counts = measure(File(File(root, "data"), meta.folder))
            total += counts
            println("%-30s %9s %10s %10s".format(
                meta.folder.take(30),
                pct(counts.multiQuotationParagraphs - counts.splitParagraphs, counts.multiQuotationParagraphs),
                pct(counts.adjacentPairs - counts.adjacentSameSpeaker, counts.adjacentPairs),
                pct(counts.alternationHolds, counts.alternationChances),
            ))
        }

        println()
        println("One speaker per paragraph (CMOS: the paragraph break is the turn boundary)")
        say(total.multiQuotationParagraphs - total.splitParagraphs, total.multiQuotationParagraphs,
            "paragraphs holding more than one quotation are all one voice")

        println("\nA paragraph break means the speaker changed")
        say(total.adjacentPairs - total.adjacentSameSpeaker, total.adjacentPairs,
            "adjoining dialogue paragraphs are two different speakers")

        println("\nTurn-taking: A-B-? continues as A")
        say(total.alternationHolds, total.alternationChances,
            "third turns of an adjoining run go to the speaker two turns back")
        println("  This is the CEILING on the alternation rule, not its score. It is measured with")
        println("  the two previous speakers known for certain; the candidate has to infer them,")
        println("  and every inference it gets wrong costs it again here.")

        println("\nContinued speech: an unclosed quotation carries into the next paragraph")
        say(total.openQuotationsContinued, total.openQuotations,
            "paragraphs leaving a quotation open are continued by the same speaker")
        println("  The convention's own signal. Where it fires it OVERRIDES turn-taking, which is")
        println("  why a rule that ignores it does not lose one line but inverts the run after it.")
    }

    private fun say(hits: Int, of: Int, what: String) {
        println("  %s of %s %s — %s".format(hits, of, what, pct(hits, of)))
    }

    private fun pct(hits: Int, of: Int) = if (of == 0) "—" else "%.1f%%".format(100.0 * hits / of)

    private fun measure(novelDir: File): Counts {
        val counts = Counts()
        val (paragraphs, gold) = Pdnc.locate(novelDir)
        val (questions, _) = Bakeoff.questions(paragraphs, gold)
        val textOf = paragraphs.associate { it.unit.index to it.unit.text }

        val turns = questions.groupBy { it.paragraph }.toSortedMap().map { (paragraph, quotations) ->
            val speakers = quotations.map { it.gold }.toSet()
            if (quotations.size > 1) {
                counts.multiQuotationParagraphs++
                if (speakers.size > 1) counts.splitParagraphs++
            }
            Turn(paragraph, quotations.first().gold, split = speakers.size > 1)
        }

        for (i in turns.indices) {
            val here = turns[i]

            // A paragraph that leaves its quotation open, followed by one that opens another:
            // the convention says these are one utterance by one speaker.
            if (textOf[here.paragraph]?.let(::opensAndLeavesOpen) == true) {
                val next = turns.getOrNull(i + 1)?.takeIf { it.paragraph == here.paragraph + 1 }
                if (next != null && textOf[next.paragraph]?.let(::opensQuotation) == true) {
                    counts.openQuotations++
                    if (next.speaker == here.speaker) counts.openQuotationsContinued++
                }
            }

            val previous = turns.getOrNull(i - 1)?.takeIf { it.paragraph == here.paragraph - 1 } ?: continue
            counts.adjacentPairs++
            if (previous.speaker == here.speaker) counts.adjacentSameSpeaker++

            // A-B-?: the two turns behind this one adjoin it and are different people, so the
            // convention claims this one is A. Continued speech is excluded — there the
            // convention claims the opposite, and counting it here would score the wrong rule.
            val before = turns.getOrNull(i - 2)?.takeIf { it.paragraph == previous.paragraph - 1 } ?: continue
            if (before.speaker == previous.speaker) continue
            if (textOf[previous.paragraph]?.let(::opensAndLeavesOpen) == true) continue
            counts.alternationChances++
            if (here.speaker == before.speaker) counts.alternationHolds++
        }
        return counts
    }

    private fun opensAndLeavesOpen(text: String) =
        opensQuotation(text) && AlternationCandidate.leavesQuotationOpen(text)

    private fun opensQuotation(text: String) = AlternationCandidate.opensQuotation(text)
}
