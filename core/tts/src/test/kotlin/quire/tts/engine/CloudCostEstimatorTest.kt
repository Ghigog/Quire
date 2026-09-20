package quire.tts.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudCostEstimatorTest {

    @Test
    fun `cost scales linearly with character count`() {
        assertEquals(1.50, CloudCostEstimator.estimate(characterCount = 10_000, pricePerThousandChars = 0.15), 1e-9)
    }

    @Test
    fun `zero characters cost nothing`() {
        assertEquals(0.0, CloudCostEstimator.estimate(characterCount = 0, pricePerThousandChars = 0.30), 1e-9)
    }

    @Test
    fun `negative inputs are rejected rather than silently producing a nonsense estimate`() {
        assertFailsWith<IllegalArgumentException> {
            CloudCostEstimator.estimate(characterCount = -1, pricePerThousandChars = 0.1)
        }
        assertFailsWith<IllegalArgumentException> {
            CloudCostEstimator.estimate(characterCount = 100, pricePerThousandChars = -0.1)
        }
    }
}
