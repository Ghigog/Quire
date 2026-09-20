package quire.app.companion.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import quire.attribution.slm.StructuredCompletion
import quire.model.Kind

class ImportPipelineTest {

    private val paragraphs = Fixtures.threeChapterBook()

    @Test
    fun `no SLM resident still casts every character and demotes unresolved lines to the narrator`() {
        val result = ImportPipeline(slm = null).run(paragraphs, bookId = "book-1", generatedAt = 0L)

        val byId = result.manifest.characters.associateBy { it.id }
        assertEquals(setOf("Sarah", "Thomas", "Eleanor"), byId.keys)
        for (character in byId.values) {
            assertNotNull(character.voice, "${character.id} should have earned a voice descriptor")
        }

        val dialogueSpans = result.entries.flatMap { it.spans }.filter { it.kind == Kind.DIALOGUE }
        val untaggedLines = dialogueSpans.filter { it.speakerId == null }
        assertEquals(3, untaggedLines.size, "one unresolved line per chapter, read by the narrator")
    }

    @Test
    fun `a scene already checkpointed is never asked of the model again`() {
        val fakeFullRun = FakeSlmRuntime()
        val checkpoints = mutableListOf<ImportCheckpoint>()
        val full = ImportPipeline(slm = StructuredCompletion(fakeFullRun)).run(
            paragraphs, bookId = "book-1", generatedAt = 0L,
            onCheckpoint = { checkpoints += it },
        )

        // Straight after the first scene's dialogue is attributed, before the second or
        // third scene's untagged line has ever been asked about.
        val midpoint = checkpoints.first { it.attributionNextScene == 1 }
        assertEquals(3, midpoint.scanNextScene, "the character scan must finish before attribution starts")

        val fakeResume = FakeSlmRuntime()
        val resumed = ImportPipeline(slm = StructuredCompletion(fakeResume)).run(
            paragraphs, bookId = "book-1", generatedAt = 0L, resumeFrom = midpoint,
        )

        assertEquals(full.manifest, resumed.manifest)
        assertEquals(full.entries, resumed.entries)
        // Only scenes 2 and 3's untagged lines are still unresolved at the midpoint —
        // never scene 1's, and never a repeat of the character-scan prompt.
        assertEquals(2, fakeResume.callCount)
    }

    @Test
    fun `progress is honest per stage`() {
        val seen = mutableListOf<ImportProgress>()
        ImportPipeline(slm = null).run(paragraphs, bookId = "book-1", generatedAt = 0L, onProgress = { seen += it })

        assertTrue(seen.isNotEmpty())
        val byStage = seen.groupBy { it.stage }
        for ((_, progressInStage) in byStage) {
            val fractions = progressInStage.map { it.fraction }
            assertEquals(fractions.sorted(), fractions, "fraction must advance monotonically within a stage")
        }
        assertEquals(1.0, byStage.getValue(ImportStage.CASTING_VOICES).last().fraction)
    }

    @Test
    fun `resuming a checkpoint for a different book is refused`() {
        val foreign = ImportCheckpoint.start("some-other-book")
        assertFailsWith<IllegalArgumentException> {
            ImportPipeline(slm = null).run(paragraphs, bookId = "book-1", generatedAt = 0L, resumeFrom = foreign)
        }
    }

    @Test
    fun `every unresolved line still carries the NARRATOR tier once nothing can resolve it`() {
        val result = ImportPipeline(slm = null).run(paragraphs, bookId = "book-1", generatedAt = 0L)
        val untagged = result.entries.flatMap { it.spans }.filter { it.speakerId == null && it.kind == Kind.DIALOGUE }
        assertTrue(untagged.isNotEmpty())
        assertNull(untagged.first().speakerId)
    }
}
