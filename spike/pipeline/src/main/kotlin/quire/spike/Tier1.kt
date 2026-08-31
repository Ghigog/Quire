package quire.spike

import quire.attribution.Heuristic
import quire.attribution.Roster
import quire.model.characters.CharacterManifest

/**
 * Adapter onto `core:attribution` (QUI-008).
 *
 * This file used to hold the rules. They now live in `core:attribution`, which is the
 * module that ships, and this keeps the spike's call sites working while running exactly
 * the implementation the product does. Two copies of the same heuristics is how the
 * measured one and the shipped one quietly stop agreeing — and while both existed, the
 * pronoun rule was reachable only by tests, never by `export`.
 */
object Tier1 {

    /** Whether adjacency alone may attribute a quote. QUI-028 scores it with this off. */
    var useActionBeats = true

    const val ADJACENCY_MIN = Roster.ADJACENCY_MIN

    /** Read the book and work out who is in it, and what they sound like. */
    fun bootstrapRoster(paragraphs: List<ParagraphUnit>): Roster.Cast =
        Roster.scan(paragraphs.map { it.locator to it.text })

    /**
     * Attribute with a full cast — genders included, so the pronoun rule can fire.
     *
     * This is the path a real book takes. Prefer it to the name-set overload, which cannot
     * resolve `she said` because it has nothing to resolve it against.
     */
    fun attribute(paragraphs: List<ParagraphUnit>, cast: Roster.Cast): List<AttributionResult> =
        attribute(paragraphs, Roster.manifest(cast, bookId = "spike", generatedAt = 0L))

    fun attribute(
        paragraphs: List<ParagraphUnit>,
        manifest: CharacterManifest,
    ): List<AttributionResult> =
        Heuristic(manifest, pronouns = true, actionBeats = useActionBeats)
            .attributeAll(paragraphs.map { it.locator to it.text })

    /** Names only, for callers that have a roster but no genders. */
    fun attribute(paragraphs: List<ParagraphUnit>, roster: Set<String>): List<AttributionResult> =
        attribute(paragraphs, manifestOf(roster))

    private fun manifestOf(names: Set<String>) = CharacterManifest(
        schemaVersion = CharacterManifest.VERSION,
        bookId = "spike", generatedAt = 0L,
        narrator = quire.model.characters.Character("narrator", "Narrator"),
        characters = names.sorted().map {
            quire.model.characters.Character(id = it, displayName = it, confidence = 1.0)
        },
    )
}
