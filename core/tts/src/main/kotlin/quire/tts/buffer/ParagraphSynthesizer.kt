package quire.tts.buffer

import quire.tts.engine.TtsChunk

/**
 * Turns one paragraph into finished audio. An implementation may wrap a single
 * [quire.tts.engine.TtsEngine] call for a single-voice paragraph, or `app/ttsservice/synthesis`'s
 * multi-voice orchestration (QUI-024) for one that mixes narration and dialogue — [RollingBuffer]
 * only cares that it gets back one [TtsChunk] per paragraph.
 *
 * [cancelled] is polled the same way [quire.tts.engine.TtsEngine.synthesize]'s is: an
 * implementation that takes a while should check it during generation, not just before
 * starting, so a seek can stop an in-flight paragraph promptly.
 */
fun interface ParagraphSynthesizer {
    /** Returns `null` if [cancelled] became true before synthesis produced any audio. */
    fun synthesize(paragraph: Paragraph, cancelled: () -> Boolean): TtsChunk?
}
