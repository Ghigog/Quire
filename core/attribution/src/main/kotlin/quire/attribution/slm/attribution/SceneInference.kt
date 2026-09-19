package quire.attribution.slm.attribution

/**
 * Tier 3, per the Requirements: "alternate between the two most recently active speakers
 * for a two-party exchange; otherwise Narrator."
 *
 * A scene counts as two-party when exactly two distinct speakers are already resolved
 * anywhere in it (Tier 1, or an earlier call's cache) — three or more active voices is
 * exactly where alternation stops meaning anything, so it declines rather than guesses.
 */
object SceneInference {

    /**
     * The speaker for the line at [index], or null if this scene does not qualify.
     *
     * @param known every already-resolved line in the scene, as (its index, its speaker),
     *   in any order.
     * @param activeSpeakers the distinct speakers among [known].
     */
    fun speakerFor(index: Int, known: List<Pair<Int, String>>, activeSpeakers: Set<String>): String? {
        if (activeSpeakers.size != 2) return null
        val (a, b) = activeSpeakers.toList()

        val before = known.lastOrNull { it.first < index }
        if (before != null) return if (before.second == a) b else a

        // No resolved line precedes this one — anchor on the next one instead, alternating
        // backwards from it, so a scene opening on an unresolved line still gets an answer.
        val after = known.firstOrNull { it.first > index }
        if (after != null) return if (after.second == a) b else a

        return null
    }
}
