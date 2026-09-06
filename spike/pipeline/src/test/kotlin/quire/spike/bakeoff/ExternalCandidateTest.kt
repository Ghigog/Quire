package quire.spike.bakeoff

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.model.Paragraph

class ExternalCandidateTest {

    private fun paragraphs(novel: String) =
        listOf(Paragraph("$novel#p0", "\"Yes,\" said Sarah.", 0, 0))

    private fun withAnswers(novel: String, body: String, block: (File) -> Unit) {
        val dir = createTempDir()
        try {
            File(dir, "$novel.answers.tsv").writeText(body)
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `reads the answers for the novel the paragraphs came from`() {
        withAnswers("Dust", "q1\tSarah\tgrimbert\nq2\tThomas\tgrimbert\n") { dir ->
            val answers = ExternalCandidate(dir).answer(paragraphs("Dust"), emptyList())
            assertEquals("Sarah", answers["q1"]?.speaker)
            assertEquals("grimbert", answers["q1"]?.evidence)
            assertEquals("Thomas", answers["q2"]?.speaker)
        }
    }

    @Test
    fun `an empty speaker is a decline, not an answer`() {
        // The distinction is the whole point of the coverage column: a quotation nobody
        // claims is read by the narrator, a quotation claimed wrongly is heard.
        withAnswers("Dust", "q1\t\tgrimbert\n") { dir ->
            assertNull(ExternalCandidate(dir).answer(paragraphs("Dust"), emptyList())["q1"])
        }
    }

    @Test
    fun `comments and blank lines are skipped`() {
        withAnswers("Dust", "# grimbert, threshold 0.5\n\nq1\tSarah\t\n") { dir ->
            val answers = ExternalCandidate(dir).answer(paragraphs("Dust"), emptyList())
            assertEquals("Sarah", answers["q1"]?.speaker)
            assertEquals("external", answers["q1"]?.evidence)   // falls back to the id
        }
    }

    @Test
    fun `a novel with no answers file answers nothing rather than failing`() {
        // A predictor that skipped a novel — ran out of memory, was interrupted — must cost
        // that novel's coverage and not the whole run.
        withAnswers("Dust", "q1\tSarah\t\n") { dir ->
            assertTrue(ExternalCandidate(dir).answer(paragraphs("Styles"), emptyList()).isEmpty())
        }
    }

    @Test
    fun `the candidate id names the model, not the mechanism`() {
        withAnswers("Dust", "") { dir ->
            assertEquals("grimbert", ExternalCandidate(dir, "grimbert").id)
        }
    }

    @Test
    fun `json escaping survives a round trip through the dump`() {
        val awkward = "He said \"no\",\tthen left.\nShe did not.\\"
        val parsed = StringBuilder()
        val s = jsonString(awkward)
        assertTrue(s.startsWith("\"") && s.endsWith("\""))
        var i = 1
        while (i < s.length - 1) {
            if (s[i] == '\\') {
                when (val c = s[i + 1]) {
                    'n' -> parsed.append('\n'); 't' -> parsed.append('\t')
                    'r' -> parsed.append('\r'); else -> parsed.append(c)
                }
                i += 2
            } else parsed.append(s[i++])
        }
        assertEquals(awkward, parsed.toString())
    }
}
