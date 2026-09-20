package quire.app.companion.voice

import quire.model.characters.CharacterManifest
import quire.tts.engine.CloudCostEstimator

/**
 * Estimates what enabling the cloud tier would cost for one book, shown before a reader
 * turns it on (QUI-042's "the reader knows what it costs before starting"). The manifest
 * records how many lines each character speaks, not how many characters — the index holds
 * the real text but reading it just to count characters is more than this estimate is worth,
 * so this multiplies by an assumed average line length instead. It is a same-order-of-magnitude
 * estimate, not a bill.
 */
object DialogueCostEstimate {
    private const val ASSUMED_CHARS_PER_LINE = 60

    fun estimatedCharacterCount(manifest: CharacterManifest): Int =
        manifest.characters.sumOf { it.lineCount } * ASSUMED_CHARS_PER_LINE

    fun estimatedCost(manifest: CharacterManifest, pricePerThousandChars: Double): Double =
        CloudCostEstimator.estimate(estimatedCharacterCount(manifest), pricePerThousandChars)
}
