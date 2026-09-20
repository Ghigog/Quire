package quire.app.companion.pipeline

import kotlinx.serialization.Serializable
import quire.attribution.scan.Enrichment
import quire.attribution.scan.ScanState
import quire.model.Kind
import quire.model.IndexEntry
import quire.model.VoiceSpan
import quire.model.characters.AgeBand
import quire.model.characters.Gender

/**
 * Everything an interrupted import needs to resume without re-billing an SLM for work it
 * already paid for.
 *
 * Two independent progress cursors, because the pipeline has two SLM-costed stages run in
 * sequence (`ImportPipeline`): [scanNextScene] is [quire.attribution.scan.BookScan]'s own
 * resumption point (QUI-007 designed it to be captured and handed back this way), and
 * [attributionNextScene] is how far the Tier 2/3 scene attribution has gotten. [entries]
 * holds every [IndexEntry] already produced for scenes before [attributionNextScene] — the
 * expensive-to-recompute half of the pipeline once an SLM is in the loop — so a resume never
 * redoes attribution work already paid for, only the cheap deterministic passes (Tier 1,
 * turn-taking) ahead of it.
 *
 * A plain, companion-owned mirror of `core:attribution`'s [ScanState]/[Enrichment] and
 * `core:model`'s [IndexEntry]/[VoiceSpan] rather than annotating those directly:
 * `@Serializable` on a core type would put a serialization library's binary contract on an
 * interface four tickets share (CLAUDE.md §2.3), for a resumption format only this ticket's
 * files need to read.
 */
@Serializable
data class ImportCheckpoint(
    val bookId: String,
    val scanNextScene: Int = 0,
    val scanEnrichment: Map<String, EnrichmentSnapshot> = emptyMap(),
    val attributionNextScene: Int = 0,
    val entries: List<IndexEntrySnapshot> = emptyList(),
) {
    fun scanState(): ScanState =
        ScanState(scanNextScene, scanEnrichment.mapValues { it.value.toEnrichment() })

    companion object {
        fun start(bookId: String) = ImportCheckpoint(bookId = bookId)
    }
}

@Serializable
data class EnrichmentSnapshot(
    val aliases: Set<String> = emptySet(),
    val gender: String = "UNKNOWN",
    val ageBand: String = "UNKNOWN",
    val traits: List<String> = emptyList(),
    val mentions: Int = 0,
)

fun Enrichment.toSnapshot() = EnrichmentSnapshot(aliases, gender.name, ageBand.name, traits, mentions)

fun EnrichmentSnapshot.toEnrichment() = Enrichment(
    aliases = aliases,
    gender = Gender.from(gender),
    ageBand = AgeBand.from(ageBand),
    traits = traits,
    mentions = mentions,
)

fun ScanState.toSnapshot(): Map<String, EnrichmentSnapshot> = enrichment.mapValues { it.value.toSnapshot() }

@Serializable
data class VoiceSpanSnapshot(
    val start: Int,
    val end: Int,
    val kind: String,
    val speakerId: String?,
    val confidence: Double,
)

@Serializable
data class IndexEntrySnapshot(
    val text: String,
    val normalized: String,
    val spans: List<VoiceSpanSnapshot>,
    val chapter: Int,
)

fun IndexEntry.toSnapshot() = IndexEntrySnapshot(
    text = text,
    normalized = normalized,
    spans = spans.map { VoiceSpanSnapshot(it.start, it.end, it.kind.name, it.speakerId, it.confidence) },
    chapter = chapter,
)

/** [seq] is assigned by the caller — a snapshot is renumbered wholesale once every scene is in. */
fun IndexEntrySnapshot.toIndexEntry(seq: Int = 0) = IndexEntry(
    seq = seq,
    text = text,
    normalized = normalized,
    spans = spans.map { VoiceSpan(it.start, it.end, Kind.valueOf(it.kind), it.speakerId, it.confidence) },
    chapter = chapter,
)
