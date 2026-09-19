package quire.attribution.scan

import quire.model.characters.AgeBand
import quire.model.characters.Gender

/**
 * One character as a single scene's SLM extraction reports them.
 *
 * Provisional across scenes on purpose — the same person may arrive under a different name
 * each time ("Miss Bennet" in one scene, "Elizabeth" in the next), which is exactly what
 * [BookScan] resolves against the heuristic pass and against characters already merged
 * from earlier scenes.
 */
data class ScannedCharacter(
    val name: String,
    val aliases: List<String> = emptyList(),
    val gender: Gender = Gender.UNKNOWN,
    val ageBand: AgeBand = AgeBand.UNKNOWN,
    val traits: List<String> = emptyList(),
)

/**
 * What the SLM pass has added for one character, folded across every scene seen so far.
 *
 * Kept apart from [quire.model.characters.Character] because a character earns a place in
 * the final manifest only after every scene has weighed in — [mentions] is what the
 * walk-on discard rule reads for a character the heuristic pass never found at all.
 */
data class Enrichment(
    val aliases: Set<String> = emptySet(),
    val gender: Gender = Gender.UNKNOWN,
    val ageBand: AgeBand = AgeBand.UNKNOWN,
    val traits: List<String> = emptyList(),
    val mentions: Int = 0,
)

/**
 * Where a scan stands: which scene it will resume from, and everything the SLM pass has
 * accumulated up to that point.
 *
 * [BookScan] does no I/O — no Android, no disk — so persisting this between an interrupted
 * scan and its resumption is the caller's job (the app layer, once QUI-001's Android
 * modules exist). This class only has to survive being held in memory and handed back.
 */
data class ScanState(
    val nextScene: Int = 0,
    val enrichment: Map<String, Enrichment> = emptyMap(),
)
