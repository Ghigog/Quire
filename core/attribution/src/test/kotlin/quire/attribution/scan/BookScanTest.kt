package quire.attribution.scan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.attribution.slm.FakeSlmRuntime
import quire.attribution.slm.StructuredCompletion
import quire.model.Paragraph
import quire.model.characters.AgeBand
import quire.model.characters.Gender

/**
 * QUI-007's Gherkin scenarios, minus "the book is readable during the scan" — that is a
 * property of the app's import flow (QUI-001's Android modules), not of this pure-Kotlin
 * scan, and has no meaning until that flow exists.
 */
class BookScanTest {

    private fun paragraph(locator: String, text: String, chapter: Int, index: Int) =
        Paragraph(locator, text, chapter, index)

    @Test
    fun `import produces a manifest, even with no model loaded`() {
        val paragraphs = listOf(paragraph("c1#p0", "\"I know,\" said Sarah.", 0, 0))

        val manifest = BookScan().scan(paragraphs, bookId = "book-1", generatedAt = 0L)

        assertTrue(manifest.characters.any { it.id == "Sarah" })
    }

    @Test
    fun `each detected character carries a gender, age band and traits from the model`() {
        val paragraphs = listOf(
            paragraph("c1#p0", "\"I know,\" said Sarah.", 0, 0),
            paragraph("c2#p0", "\"There is another train,\" said Thomas.", 1, 1),
        )
        val runtime = FakeSlmRuntime(
            listOf(
                """[{"name":"Sarah","gender":"FEMALE","ageBand":"ADULT","traits":["quiet","kind"]}]""",
                """[{"name":"Thomas","gender":"MALE","ageBand":"ADULT","traits":["gruff"]}]""",
            ),
        )

        val manifest = BookScan(StructuredCompletion(runtime))
            .scan(paragraphs, bookId = "book-1", generatedAt = 0L)

        val sarah = manifest.characters.single { it.id == "Sarah" }
        assertEquals(Gender.FEMALE, sarah.gender)
        assertEquals(AgeBand.ADULT, sarah.ageBand)
        assertEquals(listOf("quiet", "kind"), sarah.traits)

        val thomas = manifest.characters.single { it.id == "Thomas" }
        assertEquals(Gender.MALE, thomas.gender)
        assertEquals(listOf("gruff"), thomas.traits)
    }

    @Test
    fun `an alias the model reports collapses onto the heuristic-known character`() {
        val paragraphs = listOf(paragraph("c1#p0", "\"I know,\" said Elizabeth.", 0, 0))
        val runtime = FakeSlmRuntime(
            listOf("""[{"name":"Miss Bennet","aliases":["Elizabeth","Lizzy"],"gender":"FEMALE"}]"""),
        )

        val manifest = BookScan(StructuredCompletion(runtime))
            .scan(paragraphs, bookId = "book-1", generatedAt = 0L)

        // One character, not two: "Miss Bennet" folds onto the id Roster already anchored
        // to the quotation, and "Lizzy" arrives as a newly known alias of it.
        assertEquals(1, manifest.characters.size)
        val elizabeth = manifest.characters.single()
        assertEquals("Elizabeth", elizabeth.id)
        assertTrue("Miss Bennet" in elizabeth.aliases)
        assertTrue("Lizzy" in elizabeth.aliases)
    }

    @Test
    fun `a name the model sees fewer than 3 times, with no dialogue, is discarded`() {
        val paragraphs = listOf(paragraph("c1#p0", "\"I know,\" said Sarah.", 0, 0))
        // "Constable" is never anchored to a quotation by Roster (no tag, and one scene
        // cannot reach ADJACENCY_MIN), so it lives only in the SLM pass — once.
        val runtime = FakeSlmRuntime(listOf("""[{"name":"Sarah"},{"name":"Constable"}]"""))

        val manifest = BookScan(StructuredCompletion(runtime))
            .scan(paragraphs, bookId = "book-1", generatedAt = 0L)

        assertTrue(manifest.characters.none { it.id == "Constable" })
    }

    @Test
    fun `a name the model sees at least 3 times survives even without dialogue`() {
        val paragraphs = listOf(
            paragraph("c1#p0", "\"I know,\" said Sarah.", 0, 0),
            paragraph("c2#p0", "The garden was quiet.", 1, 1),
            paragraph("c3#p0", "Rain began to fall.", 2, 2),
            paragraph("c4#p0", "The lamps came on.", 3, 3),
        )
        val runtime = FakeSlmRuntime(
            listOf(
                """[{"name":"Sarah"}]""",
                """[{"name":"Constable"}]""",
                """[{"name":"Constable"}]""",
                """[{"name":"Constable"}]""",
            ),
        )

        val manifest = BookScan(StructuredCompletion(runtime))
            .scan(paragraphs, bookId = "book-1", generatedAt = 0L)

        assertTrue(manifest.characters.any { it.id == "Constable" })
    }

    @Test
    fun `a scan interrupted after one scene resumes and matches an uninterrupted run`() {
        val paragraphs = listOf(
            paragraph("c1#p0", "\"I know,\" said Sarah.", 0, 0),
            paragraph("c2#p0", "\"There is another train,\" said Thomas.", 1, 1),
        )
        val responses = listOf(
            """[{"name":"Sarah","traits":["quiet"]}]""",
            """[{"name":"Thomas","traits":["gruff"]}]""",
        )

        val straightThrough = BookScan(StructuredCompletion(FakeSlmRuntime(responses)))
            .scan(paragraphs, bookId = "book-1", generatedAt = 0L)

        var checkpoint: ScanState? = null
        BookScan(StructuredCompletion(FakeSlmRuntime(responses))).scan(
            paragraphs, bookId = "book-1", generatedAt = 0L,
            onScene = { state -> if (checkpoint == null) checkpoint = state },
        )

        // The restart: a fresh runtime holding only the *second* scene's reply. If resume
        // asked for scene 0 again, it would get this reply instead and the test would
        // catch it as a mismatch against the uninterrupted run.
        val resumed = BookScan(StructuredCompletion(FakeSlmRuntime(listOf(responses[1]))))
            .scan(paragraphs, bookId = "book-1", generatedAt = 0L, resumeFrom = checkpoint!!)

        assertEquals(straightThrough.characters, resumed.characters)
    }
}
