package quire.app.companion.pipeline

import quire.model.Paragraph

/**
 * A tiny three-chapter book: three named speakers, each explicitly tagged three times (past
 * [quire.voice.design.VoiceDesigner.MIN_LINES], so casting has enough evidence), plus one
 * untagged line per chapter that neither Tier 1 nor [quire.attribution.Conversation] can
 * resolve — three speakers already active rules out both of its inference branches — so it
 * is exactly what reaches [quire.attribution.slm.attribution.SceneAttributor] once a scene's
 * SLM call is made.
 */
object Fixtures {

    fun threeChapterBook(): List<Paragraph> {
        var index = 0
        return (0..2).flatMap { chapter ->
            listOf(
                "\"I know,\" said Sarah.",
                "\"So do I,\" said Thomas.",
                "\"As do I,\" said Eleanor.",
                "\"Nobody knows where the letter went in chapter $chapter.\"",
            ).map { text -> Paragraph("ch$chapter#p${index}", text, chapter, index++) }
        }
    }
}
