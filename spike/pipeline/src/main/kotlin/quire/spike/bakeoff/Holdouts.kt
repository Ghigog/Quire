package quire.spike.bakeoff

import quire.spike.Pdnc

/**
 * The out-of-domain split QUI-028 requires: a translated novel, genre fiction carried by
 * action beats rather than speech tags, and a first-person narrative — each scored apart
 * from the headline figure.
 *
 * **These are proxies, and the honest reading of them is bounded.** The ticket asks for
 * books *unlike PDNC*; these are books unlike the rest of PDNC, chosen by its own index
 * along one axis each. PDNC is 28 novels published 1811–1934, so "contemporary genre
 * fiction" is not in it at any price and the action-beat axis is served by Doyle, Christie
 * and Wells instead. What the split can show is a model that only works on the corpus's
 * centre of mass — third person, untranslated, English, literary. What it cannot show is
 * degradation on prose written in the last ninety years.
 *
 * The gap that the split does measure is therefore a **lower bound** on real-library
 * degradation, and must be quoted as one. [External] is the slot for closing that gap
 * properly; nothing fills it yet, because a book we could commit is a book §8 forbids.
 */
object Holdouts {

    enum class Axis(val id: String, val asks: String) {
        TRANSLATION("translation", "a translated novel"),
        ACTION_BEATS("action-beats", "genre fiction, where beats do the work speech tags do"),
        FIRST_PERSON("first-person", "a first-person narrative"),
    }

    /** PDNC's own genres for books that are not the corpus's literary centre of mass. */
    private val GENRE_FICTION = setOf("crime", "scifi", "adventure", "romance", "horror", "fantasy")

    fun axisOf(meta: Pdnc.NovelMeta): List<Axis> = buildList {
        if (meta.translated) add(Axis.TRANSLATION)
        if (meta.genre.lowercase() in GENRE_FICTION) add(Axis.ACTION_BEATS)
        if (meta.narrativePerson == 1) add(Axis.FIRST_PERSON)
    }

    fun heldOut(meta: Pdnc.NovelMeta) = axisOf(meta).isNotEmpty()

    /** The novels the headline PDNC number is computed over: everything held out is gone. */
    fun headline(index: List<Pdnc.NovelMeta>) = index.filterNot(::heldOut)

    fun byAxis(index: List<Pdnc.NovelMeta>): Map<Axis, List<Pdnc.NovelMeta>> =
        Axis.entries.associateWith { axis -> index.filter { axis in axisOf(it) } }

    /**
     * A book from outside PDNC entirely, scored on the same axes.
     *
     * Not implemented, and deliberately not faked. Writing our own passages would produce a
     * number built from the cases we thought of, which is the bias
     * `fixtures/attribution/README.md` warns about and which QUI-018 already paid for once —
     * hand-written fixtures said 100% precision where PDNC said 58.5%. The slot is described
     * so that a real book can be dropped in without touching the harness: a labelled TSV per
     * book under `fixtures/attribution/holdout/`, in the format `Fixture` already reads,
     * naming its axis in a `# axis:` header. Getting those books is the blocker — Gutenberg
     * is unreachable from CI and CLAUDE.md §8 forbids committing book text either way.
     */
    object External {
        const val DIRECTORY = "fixtures/attribution/holdout"
    }
}
