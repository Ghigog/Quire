package quire.tts.engine

/**
 * Turns a character count into an estimated spend, so a reader sees a number before
 * enabling the cloud tier for a book (QUI-042). Quire holds no account with any provider
 * and cannot look up real pricing — [pricePerThousandChars] is what the reader typed in
 * from their own provider's rate card.
 */
object CloudCostEstimator {
    fun estimate(characterCount: Int, pricePerThousandChars: Double): Double {
        require(characterCount >= 0) { "characterCount cannot be negative" }
        require(pricePerThousandChars >= 0) { "pricePerThousandChars cannot be negative" }
        return characterCount / 1000.0 * pricePerThousandChars
    }
}
