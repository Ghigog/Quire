package quire.spike.slice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import quire.index.InMemoryBookIndex
import quire.index.OffsetMap
import quire.index.Matcher
import quire.index.Normalizer
import quire.model.IndexEntry
import quire.model.Kind
import quire.model.VoiceSpan

class ChunkPlanTest {

    // "You have been avoiding the letter," she said.
    //  ^-------------- Sarah --------------^^-- narrator --^
    private val line = "\"You have been avoiding the letter,\" she said."

    private fun entry(seq: Int, text: String, spans: List<VoiceSpan>) =
        IndexEntry(seq, text, Normalizer.normalize(text), spans)

    private val book = InMemoryBookIndex("t", listOf(
        entry(0, "Thomas did not turn from the desk.",
            listOf(VoiceSpan(0, 34, Kind.NARRATION, null, 1.0))),
        entry(1, line, listOf(
            VoiceSpan(0, 36, Kind.DIALOGUE, "Sarah", 1.0),
            VoiceSpan(36, line.length, Kind.NARRATION, null, 1.0),
        )),
    ))

    private val casting = Casting(listOf("Sarah"), voiceCount = 904)

    private fun plan(vararg chunks: String): List<List<Segment>> {
        val matcher = Matcher(book)
        return chunks.map { ChunkPlan.of(it, matcher.match(it), casting) }
    }

    @Test
    fun `the speech clause is the character, with no quote mark in it`() {
        val (_, speech) = plan("Thomas did not turn from the desk.", "\"You have been")
        assertEquals(listOf("Sarah"), speech.map { it.speakerId }.distinct())
    }

    @Test
    fun `the continuation clause is still the character`() {
        val (_, _, cont) = plan(
            "Thomas did not turn from the desk.", "\"You have been", " avoiding the letter,")
        assertEquals(listOf("Sarah"), cont.map { it.speakerId }.distinct())
    }

    @Test
    fun `the speech tag after the closing quote is the narrator, not the character`() {
        // The case a naive implementation gets wrong: this chunk's match carries both
        // spans, so taking the first would read "she said." in Sarah's voice.
        val (_, _, _, tag) = plan(
            "Thomas did not turn from the desk.", "\"You have been",
            " avoiding the letter,", " she said.")
        assertEquals(listOf<String?>(null), tag.map { it.speakerId }.distinct())
        assertEquals(casting.narrator, tag.single().voice)
    }

    @Test
    fun `a whole line containing both is split into two voices in order`() {
        val (_, whole) = plan("Thomas did not turn from the desk.", line)
        assertTrue(ChunkPlan.switchesVoice(whole), "expected a voice change, got $whole")
        assertEquals(listOf("Sarah", null), whole.map { it.speakerId })
        // Every character of the host's chunk is spoken exactly once, in order.
        assertEquals(line, whole.joinToString("") { it.text })
    }

    @Test
    fun `an unmatched chunk reads in the narrator's voice`() {
        val (stray) = plan("A sentence from a different book entirely.")
        assertEquals(1, stray.size)
        assertEquals(casting.narrator, stray.single().voice)
    }

    @Test
    fun `characters are cast away from the narrator and from each other`() {
        val cast = Casting(listOf("Sarah", "Thomas", "Mr Ashcombe"), voiceCount = 904).cast
        assertEquals(3, cast.values.distinct().size)
        assertTrue(cast.values.all { it >= 452 }, "characters crowd the narrator: $cast")
    }

    @Test
    fun `the offset map reproduces the normaliser`() {
        for (text in listOf(line, "CHAPTER TWO", "don't — really?", "  spaced  out  ")) {
            assertTrue(OffsetMap(text).faithful, "drifted on: $text")
        }
    }
}
