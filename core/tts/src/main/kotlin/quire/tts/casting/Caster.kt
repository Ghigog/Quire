package quire.tts.casting

import quire.model.characters.Character
import quire.model.characters.CharacterManifest

/**
 * QUI-011: a deterministic caster. Maps every character in a manifest to a voice, using
 * gender, age band and line count, and guarantees that characters who share a scene never
 * sound identical.
 *
 * **What this deliberately does not do.** It has no notion of pitch or a generated blend —
 * that is `core:voice`'s [VoicePool]-supplying business, composed by the caller (the app
 * layer, once QUI-001's modules exist), the same split QUI-007 used for job C. This class
 * only decides *which slot* of an already-given pool each character gets, and whether two
 * co-present characters collide.
 */
object Caster {

    /**
     * @param coPresence one entry per scene: the character ids who appear in it together.
     *   Two characters in the same entry must never receive the same (voiceId, rate).
     * @param previous a prior cast for this book, if one exists. Any [AssignmentSource.USER]
     *   entry in it is copied verbatim and never reconsidered — a reader's choice outlives
     *   a rescan.
     */
    fun cast(
        manifest: CharacterManifest,
        coPresence: List<Set<String>>,
        pool: VoicePool,
        previous: Cast? = null,
    ): Cast {
        val conflicts = conflictGraph(coPresence)
        val assigned = mutableMapOf<String, VoiceAssignment>()
        previous?.voices?.filterValues { it.source == AssignmentSource.USER }
            ?.let(assigned::putAll)

        // Priority by line count: the most-spoken characters claim their pool's best-spread
        // slots first, so a scarce pool shortchanges walk-ons rather than leads.
        val priority = manifest.characters.sortedWith(
            compareByDescending<Character> { it.lineCount }.thenBy { it.id },
        )
        val candidatesOf = priority.associateWith { pool.candidatesFor(it.gender, it.ageBand) }
        val groupSize = candidatesOf.values.groupingBy { it }.eachCount()
        val seenInGroup = mutableMapOf<List<Int>, Int>()

        for (character in priority) {
            if (character.id in assigned) continue // a USER override, preserved above
            val candidates = candidatesOf.getValue(character)
            val of = groupSize.getValue(candidates)
            val nth = seenInGroup.getOrDefault(candidates, 0)
            seenInGroup[candidates] = nth + 1

            assigned[character.id] = resolve(character.id, candidates, nth, of, conflicts, assigned)
        }

        val narratorCandidates = pool.candidatesFor(manifest.narrator.gender, manifest.narrator.ageBand)
        val narrator = previous?.narratorVoiceId
            ?: narratorCandidates.getOrNull(narratorCandidates.size / 2)
            ?: 0

        return Cast(manifest.bookId, narrator, assigned)
    }

    private fun resolve(
        characterId: String,
        candidates: List<Int>,
        nth: Int,
        of: Int,
        conflicts: Map<String, Set<String>>,
        assigned: Map<String, VoiceAssignment>,
    ): VoiceAssignment {
        if (candidates.isEmpty()) return VoiceAssignment(0)

        val neighbours = conflicts[characterId].orEmpty()
        val preferred = spreadIndex(nth, of, candidates.size)
        val busyVoiceIds = neighbours.mapNotNull { assigned[it]?.voiceId }.toSet()

        val freeIndex = searchOrder(preferred, candidates.size)
            .firstOrNull { candidates[it] !in busyVoiceIds }
        if (freeIndex != null) return VoiceAssignment(candidates[freeIndex])

        // The pool has fewer usable ids than this character's scene has speakers. Reuse is
        // forced, so differentiate by rate rather than ever sounding identical to a
        // co-present neighbour — the Requirements' explicit order of preference.
        val voiceId = candidates[preferred]
        val busyRates = neighbours
            .mapNotNull { assigned[it] }
            .filter { it.voiceId == voiceId }
            .map { it.rate }
            .toSet()
        return VoiceAssignment(voiceId, rate = nextFreeRate(busyRates))
    }

    /** Every character id that shares at least one scene with another, both ways. */
    private fun conflictGraph(coPresence: List<Set<String>>): Map<String, Set<String>> {
        val graph = mutableMapOf<String, MutableSet<String>>()
        for (scene in coPresence) {
            for (a in scene) for (b in scene) {
                if (a != b) graph.getOrPut(a) { mutableSetOf() }.add(b)
            }
        }
        return graph
    }

    /**
     * Where the `nth` of `of` characters sharing one pool lands within it — spread across
     * the middle rather than packed at one end, so two characters land roughly a third and
     * two-thirds of the way in rather than at adjacent, easily-confused ids.
     */
    private fun spreadIndex(nth: Int, of: Int, size: Int): Int {
        if (size <= 1) return 0
        if (of <= 1) return size / 2
        val fraction = (nth + 1).toDouble() / (of + 1)
        return (fraction * (size - 1)).toInt().coerceIn(0, size - 1)
    }

    /** [preferred] first, then outward alternately — the nearest free slot to the ideal one. */
    private fun searchOrder(preferred: Int, size: Int): List<Int> {
        val order = mutableListOf(preferred)
        var offset = 1
        while (order.size < size) {
            if (preferred + offset < size) order += preferred + offset
            if (preferred - offset >= 0) order += preferred - offset
            offset++
        }
        return order
    }

    /** 1.0, then alternating steps away from it, until one isn't already in [used]. */
    private fun nextFreeRate(used: Set<Double>): Double {
        if (1.0 !in used) return 1.0
        var step = 1
        while (true) {
            val up = 1.0 + step * RATE_STEP
            if (up !in used) return up
            val down = 1.0 - step * RATE_STEP
            if (down !in used) return down
            step++
        }
    }

    private const val RATE_STEP = 0.05
}
