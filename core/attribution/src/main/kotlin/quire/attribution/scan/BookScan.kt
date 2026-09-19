package quire.attribution.scan

import quire.attribution.Roster
import quire.attribution.scenes.SceneSegmenter
import quire.attribution.slm.StructuredCompletion
import quire.attribution.slm.StructuredResult
import quire.model.Paragraph
import quire.model.characters.AgeBand
import quire.model.characters.Character
import quire.model.characters.CharacterManifest
import quire.model.characters.Gender

/**
 * QUI-007: reads a book once at import and writes the cast it finds.
 *
 * Two passes, in order:
 *
 * 1. **The heuristic pass** ([Roster]) — free, whole-book, and the only pass this class can
 *    run without a loaded model. It supplies every character's baseline name and gender,
 *    and it is what already discards a walk-on glimpsed only in passing: its own
 *    `ADJACENCY_MIN` (8 sightings) is stricter than this ticket's 3.
 * 2. **The SLM pass**, [slm] permitting — it is not always resident (QUI-031), and a
 *    manifest with names and genders but no traits is still usable. One call per
 *    [SceneSegmenter] scene rather than per line: ~3,000 unresolved quotations in a novel
 *    makes a per-line budget unreachable (QUI-032's Worklog measured this). It adds what
 *    the heuristic pass structurally cannot see — aliases, age band, traits, and a
 *    character known only by a name no speech tag or pronoun ever anchors to a quotation.
 *
 * **Resumable by construction, not by side effect.** This class does no I/O of its own — no
 * Android, no disk (`core:attribution` is pure Kotlin/JVM) — so "resume" means the caller
 * can capture the [ScanState] handed to [onScene] after any scene and pass it back in as
 * [resumeFrom] after a restart. Already-processed scenes are skipped, not re-billed against
 * the model; only the heuristic pass re-runs, and it costs milliseconds on a whole book.
 */
class BookScan(private val slm: StructuredCompletion? = null) {

    fun scan(
        paragraphs: List<Paragraph>,
        bookId: String,
        generatedAt: Long,
        resumeFrom: ScanState? = null,
        onScene: (ScanState) -> Unit = {},
    ): CharacterManifest {
        val cast = Roster.scan(paragraphs.map { it.locator to it.text })
        var state = resumeFrom ?: ScanState()

        if (slm != null) {
            val scenes = SceneSegmenter.segment(paragraphs)
            for (index in state.nextScene until scenes.size) {
                val scene = scenes[index]
                val text = paragraphs.filter { it.index in scene }.joinToString("\n") { it.text }
                val found = extract(text, cast.names)
                state = ScanState(nextScene = index + 1, enrichment = fold(state.enrichment, found, cast.names))
                onScene(state)
            }
        }

        return assemble(cast, state.enrichment, bookId, generatedAt)
    }

    private fun extract(sceneText: String, knownNames: Set<String>): List<ScannedCharacter> {
        val runtime = slm ?: return emptyList()
        // Declines rather than guesses on a malformed reply, the same asymmetry Tier 1
        // applies to an unresolved line: a character this scene misses is merely absent, a
        // fabricated one is heard.
        return when (val result = runtime.complete(prompt(sceneText, knownNames), CharacterExtractionShape, MAX_TOKENS)) {
            is StructuredResult.Success -> result.value
            is StructuredResult.Failure -> emptyList()
        }
    }

    private fun prompt(sceneText: String, knownNames: Set<String>) = buildString {
        appendLine("List every named character present in this scene, as a JSON array of")
        appendLine("objects with fields: name, aliases (array), gender (MALE/FEMALE/NEUTRAL/")
        appendLine("UNKNOWN), ageBand (CHILD/TEEN/ADULT/ELDER/UNKNOWN), traits (array, at")
        appendLine("most 3 short adjectives). Use a name already on this list when the scene")
        appendLine("clearly refers to the same person under a different name or title:")
        appendLine(knownNames.sorted().joinToString(", "))
        appendLine()
        append(sceneText)
    }

    /**
     * Folds one scene's [found] characters into the running [enrichment], by canonical id.
     *
     * [knownNames] — Roster's heuristic cast — is consulted on every scene, not only
     * folded in once at the start: it is what lets a scene's alias collapse onto a
     * character the heuristic pass already anchored to a quotation, which is the case this
     * ticket's alias-collapsing scenario actually tests.
     */
    private fun fold(
        enrichment: Map<String, Enrichment>,
        found: List<ScannedCharacter>,
        knownNames: Set<String>,
    ): Map<String, Enrichment> {
        val established = knownNames.associateBy { it.lowercase() } +
            enrichment.flatMap { (id, e) -> (e.aliases + id).map { it.lowercase() to id } }
        val result = enrichment.toMutableMap()
        for (character in found) {
            val id = canonicalId(character, established + result.keys.associateBy { it.lowercase() })
            val previous = result[id]
            val mentionedAliases = (character.aliases + character.name).map { it.trim() }.filterNot { it.equals(id, ignoreCase = true) }
            result[id] = Enrichment(
                aliases = previous?.aliases.orEmpty() + mentionedAliases,
                gender = previous?.gender?.takeIf { it != Gender.UNKNOWN } ?: character.gender,
                ageBand = previous?.ageBand?.takeIf { it != AgeBand.UNKNOWN } ?: character.ageBand,
                traits = (previous?.traits.orEmpty() + character.traits).distinct().take(MAX_TRAITS),
                mentions = (previous?.mentions ?: 0) + 1,
            )
        }
        return result
    }

    /**
     * The character [scanned] refers to, as an id already known to this book — from the
     * heuristic pass or from an earlier scene's extraction — or, failing that, its own name
     * as a newly discovered id.
     *
     * Tries every name the scene offered (the primary name, then each alias) against every
     * name already established, exact first and then by substring — the same "Ashcombe"
     * inside "Mr Ashcombe" rule [quire.attribution.Heuristic.known] uses, for the same
     * reason: a title or honorific should not defeat a match the book itself would allow.
     */
    private fun canonicalId(scanned: ScannedCharacter, established: Map<String, String>): String {
        val candidates = listOf(scanned.name) + scanned.aliases
        for (candidate in candidates) {
            established[candidate.trim().lowercase()]?.let { return it }
        }
        for (candidate in candidates) {
            val folded = candidate.trim().lowercase()
            established.entries.firstOrNull { (known, _) -> folded.endsWith(known) || known.endsWith(folded) }
                ?.let { return it.value }
        }
        return scanned.name.trim()
    }

    private fun assemble(
        cast: Roster.Cast,
        enrichment: Map<String, Enrichment>,
        bookId: String,
        generatedAt: Long,
    ): CharacterManifest {
        val baseline = Roster.manifest(cast, bookId, generatedAt)
        val knownIds = baseline.characters.map { it.id }.toSet()

        // Characters the SLM pass found that the heuristic pass never anchored to a
        // quotation at all. The walk-on rule applies to these alone: everyone already in
        // [baseline] passed Roster's own, stricter bar (a speech tag, or ADJACENCY_MIN
        // sightings) to get there.
        val discovered = (enrichment.keys - knownIds).mapNotNull { id ->
            val e = enrichment.getValue(id)
            if (e.mentions < WALKON_MIN) null
            else Character(id = id, displayName = id, gender = e.gender, confidence = 0.4)
        }

        val enriched = (baseline.characters + discovered).map { character ->
            val e = enrichment[character.id] ?: return@map character
            character.copy(
                aliases = (character.aliases + e.aliases).distinct().filterNot { it.equals(character.id, ignoreCase = true) },
                gender = character.gender.takeIf { it != Gender.UNKNOWN } ?: e.gender,
                ageBand = if (e.ageBand != AgeBand.UNKNOWN) e.ageBand else character.ageBand,
                traits = e.traits.ifEmpty { character.traits },
            )
        }

        return baseline.copy(characters = enriched.sortedBy { it.displayName })
    }

    companion object {
        /** A scene rarely needs more than this to list its speakers; keeps a call bounded. */
        const val MAX_TOKENS = 512

        /** Matches [quire.voice.design.VoiceDesigner.MIN_LINES]: fewer mentions, no place in the cast. */
        const val WALKON_MIN = 3

        private const val MAX_TRAITS = 3
    }
}
