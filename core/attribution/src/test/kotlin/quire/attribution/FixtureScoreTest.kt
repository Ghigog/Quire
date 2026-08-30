package quire.attribution

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import quire.model.Kind
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender

/**
 * What Tier 1 actually resolves, on the labelled fixtures rather than on examples chosen
 * to make it look good.
 *
 * Coverage and precision are reported separately and neither is optional. High precision at
 * low coverage means most of a book reads in the narrator's voice; high coverage at low
 * precision means characters speak in each other's voices, which is worse. QUI-028 found
 * Tier 1 guessing and losing on untagged material, which is the failure this scoring is
 * here to keep visible.
 */
class FixtureScoreTest {

    private data class Score(val gold: Int, val attributed: Int, val correct: Int) {
        val coverage get() = if (gold == 0) 0.0 else attributed.toDouble() / gold
        val precision get() = if (attributed == 0) 0.0 else correct.toDouble() / attributed
    }

    private fun load(name: String): Pair<CharacterManifest, List<Pair<String, String>>> {
        val file = File("../../fixtures/attribution/$name.tsv")
        val lines = file.readLines()
        val cast = lines.firstOrNull { it.startsWith("# cast:") }
            ?.removePrefix("# cast:")?.split(',')
            ?.mapNotNull {
                val parts = it.split('=', limit = 2).map(String::trim)
                if (parts.size == 2) parts[0] to Gender.from(parts[1]) else null
            }?.toMap().orEmpty()

        val rows = lines
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { it.split('\t', limit = 2) }
            .map { (who, text) -> who to text }

        val manifest = CharacterManifest(
            schemaVersion = CharacterManifest.VERSION, bookId = name, generatedAt = 0,
            narrator = Character("narrator", "Narrator", gender = Gender.NEUTRAL),
            characters = cast.map { (n, g) -> Character(id = n, displayName = n, gender = g, confidence = 1.0) },
        )
        return manifest to rows
    }

    private fun score(name: String, pronouns: Boolean = true): Score {
        val (manifest, rows) = load(name)
        val heuristic = Heuristic(manifest, pronouns = pronouns)
        var gold = 0
        var attributed = 0
        var correct = 0
        for ((who, text) in rows) {
            // Rows the fixture itself marks unresolvable, or that name two possible
            // speakers, are not a fair target for a heuristic and are skipped.
            if (who == "NARRATION" || who == "UNKNOWN" || '|' in who) continue
            gold++
            val dialogue = heuristic.attribute("p", text).filter { it.kind == Kind.DIALOGUE }
            val guess = dialogue.firstNotNullOfOrNull { it.speakerId } ?: continue
            attributed++
            if (guess == who) correct++
        }
        return Score(gold, attributed, correct)
    }

    @Test
    fun `the pronoun rule is scored against its own absence`() {
        var withGold = 0; var withAttr = 0; var withCorrect = 0
        var withoutAttr = 0; var withoutCorrect = 0
        for (name in listOf("tagged", "untagged", "beats")) {
            val on = score(name, pronouns = true)
            val off = score(name, pronouns = false)
            withGold += on.gold; withAttr += on.attributed; withCorrect += on.correct
            withoutAttr += off.attributed; withoutCorrect += off.correct
        }
        val onCov = withAttr * 100.0 / withGold
        val offCov = withoutAttr * 100.0 / withGold
        println("pronoun rule OFF: coverage %.1f%%, precision %.1f%%".format(
            offCov, withoutCorrect * 100.0 / withoutAttr))
        println("pronoun rule ON:  coverage %.1f%%, precision %.1f%%".format(
            onCov, withCorrect * 100.0 / withAttr))
        println("lift: %+.1f points of coverage, %+d lines".format(onCov - offCov, withAttr - withoutAttr))

        // The rule must add coverage without buying it with wrong answers.
        assertTrue(withAttr > withoutAttr, "the pronoun rule resolved nothing")
        assertTrue(
            withCorrect - withoutCorrect == withAttr - withoutAttr,
            "the pronoun rule added ${withAttr - withoutAttr} lines but only " +
                "${withCorrect - withoutCorrect} were right",
        )
    }

    @Test
    fun `report coverage and precision on every labelled fixture`() {
        var totalGold = 0
        var totalAttributed = 0
        var totalCorrect = 0
        println("fixture      gold  attributed  correct  coverage  precision")
        for (name in listOf("tagged", "untagged", "beats")) {
            val s = score(name)
            totalGold += s.gold; totalAttributed += s.attributed; totalCorrect += s.correct
            println("%-11s %5d %11d %8d %8.1f%% %9.1f%%".format(
                name, s.gold, s.attributed, s.correct, s.coverage * 100, s.precision * 100))
        }
        val coverage = totalAttributed.toDouble() / totalGold
        val precision = totalCorrect.toDouble() / totalAttributed
        println("%-11s %5d %11d %8d %8.1f%% %9.1f%%".format(
            "ALL", totalGold, totalAttributed, totalCorrect, coverage * 100, precision * 100))

        // Guards, not targets. They exist so a change that quietly trades one for the other
        // fails here rather than on a device three weeks later.
        assertTrue(precision >= 0.80, "precision fell to %.1f%%".format(precision * 100))
        assertTrue(coverage >= 0.40, "coverage fell to %.1f%%".format(coverage * 100))
    }
}
