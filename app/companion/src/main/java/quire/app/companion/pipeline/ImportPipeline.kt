package quire.app.companion.pipeline

import quire.attribution.Conversation
import quire.attribution.Heuristic
import quire.attribution.scan.BookScan
import quire.attribution.scenes.SceneSegmenter
import quire.attribution.slm.StructuredCompletion
import quire.attribution.slm.attribution.SceneAttributor
import quire.model.AttributionResult
import quire.model.IndexEntry
import quire.model.Paragraph
import quire.model.Tier
import quire.model.characters.CharacterManifest
import quire.voice.foundry.SpeakerProfile

/**
 * QUI-025: the companion's whole import, from parsed paragraphs to a cast and an index.
 *
 * No I/O of its own — no Android, no disk — same discipline [BookScan] holds itself to
 * (CLAUDE.md §9). [quire.app.companion.ImportService] is the thin glue that reads the EPUB,
 * drives this, and writes what it returns; this class is what a JVM unit test exercises
 * without a device.
 *
 * The order matters, same as `spike/ttsbinding`'s `BookImport`: the cast has to exist before
 * dialogue can be attributed to anyone, and Tier 1 has to run before casting has any explicit
 * lines to design a voice from.
 *
 * **Resumable by construction.** Every scene this class finishes — in either the scan or the
 * attribution pass — is reported through [onCheckpoint] before the next one starts, so a
 * caller that persists each checkpoint (see [CheckpointStore]) can hand the last one back in
 * as [resumeFrom] after a restart and never pay an SLM twice for the same scene.
 */
class ImportPipeline(
    private val slm: StructuredCompletion? = null,
    private val speakerProfile: SpeakerProfile = SpeakerProfile(emptyList()),
) {
    data class Result(val manifest: CharacterManifest, val entries: List<IndexEntry>)

    fun run(
        paragraphs: List<Paragraph>,
        bookId: String,
        generatedAt: Long,
        resumeFrom: ImportCheckpoint? = null,
        onProgress: (ImportProgress) -> Unit = {},
        onCheckpoint: (ImportCheckpoint) -> Unit = {},
    ): Result {
        require(paragraphs.isNotEmpty()) { "no paragraphs to import" }
        val start = resumeFrom ?: ImportCheckpoint.start(bookId)
        require(start.bookId == bookId) { "checkpoint is for ${start.bookId}, not $bookId" }
        var checkpoint = start

        val scenes = SceneSegmenter.segment(paragraphs)
        val totalScenes = scenes.size.coerceAtLeast(1)

        onProgress(ImportProgress(ImportStage.SCANNING_CAST, checkpoint.scanNextScene.toDouble() / totalScenes))
        val manifest = BookScan(slm).scan(
            paragraphs = paragraphs,
            bookId = bookId,
            generatedAt = generatedAt,
            resumeFrom = checkpoint.scanState(),
            onScene = { state ->
                checkpoint = checkpoint.copy(scanNextScene = state.nextScene, scanEnrichment = state.toSnapshot())
                onCheckpoint(checkpoint)
                onProgress(ImportProgress(ImportStage.SCANNING_CAST, state.nextScene.toDouble() / totalScenes))
            },
        )
        onProgress(ImportProgress(ImportStage.SCANNING_CAST, 1.0))

        onProgress(ImportProgress(ImportStage.ATTRIBUTING_DIALOGUE, checkpoint.attributionNextScene.toDouble() / totalScenes))
        val tier1 = Heuristic(manifest).attributeAll(paragraphs.map { it.locator to it.text })
        val afterConversation = Conversation.resolve(tier1, cast = manifest.characters.map { it.id })
        val byParagraph = afterConversation.groupBy { it.locator.substringBeforeLast("#s") }
        val paragraphsByIndex = paragraphs.associateBy { it.index }
        val knownIds = manifest.characters.map { it.id }.toSet()
        val attributor = slm?.let { SceneAttributor(it, knownIds) }

        val entries = checkpoint.entries.mapIndexed { i, e -> e.toIndexEntry(i) }.toMutableList()
        for (sceneIndex in checkpoint.attributionNextScene until scenes.size) {
            val scene = scenes[sceneIndex]
            val sceneParagraphs = (scene.start until scene.endExclusive).mapNotNull { paragraphsByIndex[it] }
            val sceneLines = sceneParagraphs.flatMap { byParagraph[it.locator].orEmpty() }
            val resolved = if (attributor != null) attributor.attribute(sceneLines) else demote(sceneLines)

            entries += IndexEntryAssembly.assemble(sceneParagraphs, resolved)
            checkpoint = checkpoint.copy(
                attributionNextScene = sceneIndex + 1,
                entries = entries.mapIndexed { i, e -> e.copy(seq = i).toSnapshot() },
            )
            onCheckpoint(checkpoint)
            onProgress(ImportProgress(ImportStage.ATTRIBUTING_DIALOGUE, (sceneIndex + 1).toDouble() / totalScenes))
        }
        val renumbered = entries.mapIndexed { i, e -> e.copy(seq = i) }

        onProgress(ImportProgress(ImportStage.CASTING_VOICES, 0.0))
        val cast = VoiceCasting(speakerProfile).assign(manifest, afterConversation)
        onProgress(ImportProgress(ImportStage.CASTING_VOICES, 1.0))

        return Result(cast, renumbered)
    }

    /** No SLM resident (QUI-031 has not picked a runtime yet): every Tier 1 decline reads as narration. */
    private fun demote(lines: List<AttributionResult>) = lines.map {
        if (it.tier == Tier.NONE) it.copy(speakerId = null, confidence = 0.0, tier = Tier.NARRATOR, evidence = "no SLM resident")
        else it
    }
}
