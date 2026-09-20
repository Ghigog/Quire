package quire.tts.buffer

import quire.tts.engine.TtsChunk
import java.util.concurrent.atomic.AtomicInteger

/** [totalParagraphs] paragraphs of trivial text, then `null` — a small fake "book". */
class FakeParagraphSource(private val totalParagraphs: Int) : ParagraphSource {
    override fun at(index: Int): Paragraph? =
        if (index in 0 until totalParagraphs) Paragraph(index, "paragraph $index") else null
}

/**
 * Returns a fixed-shape [TtsChunk] instantly, unless told to [blockIndex] until [release] is
 * called — the hook the seek-cancellation test uses to catch a synthesis mid-flight.
 * [maxConcurrent] records the highest number of overlapping [synthesize] calls, so a test can
 * assert the ring buffer never runs two syntheses at once.
 */
class FakeParagraphSynthesizer(
    private val sampleRate: Int = 16_000,
    private val samplesPerParagraph: Int = 1_600,
) : ParagraphSynthesizer {
    private val concurrent = AtomicInteger(0)
    val maxConcurrent = AtomicInteger(0)
    var blockIndex: Int? = null
    @Volatile private var released = false

    fun release() {
        released = true
    }

    override fun synthesize(paragraph: Paragraph, cancelled: () -> Boolean): TtsChunk? {
        val now = concurrent.incrementAndGet()
        maxConcurrent.updateAndGet { current -> maxOf(current, now) }
        try {
            if (paragraph.index == blockIndex) {
                while (!released) {
                    if (cancelled()) return null
                    Thread.sleep(1)
                }
            }
            if (cancelled()) return null
            return TtsChunk(FloatArray(samplesPerParagraph), sampleRate, voiceId = 0, boundaries = emptyList())
        } finally {
            concurrent.decrementAndGet()
        }
    }
}
