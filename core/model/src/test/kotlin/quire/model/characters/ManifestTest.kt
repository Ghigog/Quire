package quire.model.characters

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class ManifestTest {

    private fun example() = checkNotNull(
        javaClass.getResourceAsStream("/characters.example.json"),
    ) { "the example manifest is the fixture every downstream ticket tests against" }
        .readBytes().decodeToString()

    @Test
    fun `round trip preserves content, including unknown fields`() {
        // A field from a future schema: a newer companion app writing through an older
        // service must get its data back, not a silently trimmed manifest.
        val withExtras = example().replace(
            "\"lineCount\": 118",
            "\"lineCount\": 118,\n      \"voiceHint\": { \"pitch\": -2 }",
        ).replace(
            "\"generatedAt\": 1756483200000,",
            "\"generatedAt\": 1756483200000,\n  \"scannerBuild\": \"2027.4\",",
        )

        val once = ManifestCodec.decode(withExtras)
        val twice = ManifestCodec.decode(ManifestCodec.encode(once))
        assertEquals(once, twice)

        assertTrue("scannerBuild" in once.extras)
        assertTrue("voiceHint" in once.characters.first { it.id == "sarah" }.extras)

        // Semantically identical as JSON, not merely as our own data class.
        assertEquals(
            Json.parseToJsonElement(withExtras).jsonObject,
            Json.parseToJsonElement(ManifestCodec.encode(once)).jsonObject,
        )
    }

    @Test
    fun `a manifest with voice descriptors round trips`() {
        val withVoice = example().replace(
            "\"lineCount\": 118",
            """"lineCount": 118,
      "voice": {
        "speakerId": 447,
        "espeakVoice": "en-GB-scotland",
        "lengthScale": 1.05,
        "targetF0Hz": 118.0,
        "description": "Laconic, low, unhurried. Rarely raises his voice.",
        "source": "auto"
      }""",
        )

        val once = ManifestCodec.decode(withVoice)
        val twice = ManifestCodec.decode(ManifestCodec.encode(once))
        assertEquals(once, twice)

        val sarah = once.characters.first { it.id == "sarah" }.voice
        assertEquals(447, sarah?.speakerId)
        assertEquals("en-GB-scotland", sarah?.espeakVoice)
        assertEquals(1.05, sarah?.lengthScale)
        assertEquals(118.0, sarah?.targetF0Hz)
        assertEquals("Laconic, low, unhurried. Rarely raises his voice.", sarah?.description)
        assertEquals(VoiceSource.AUTO, sarah?.source)

        assertNull(once.characters.first { it.id == "thomas" }.voice, "voice is absent, not defaulted, when nobody wrote one")
    }

    @Test
    fun `a manifest without voice descriptors still loads, voice absent`() {
        val manifest = ManifestCodec.decode(example())
        assertNull(manifest.narrator.voice)
        manifest.characters.forEach { assertNull(it.voice) }
    }

    @Test
    fun `a field inside voice this reader does not know still survives a round trip`() {
        // Modelling an *older* reader here: this schema's own additionalProperties rule means
        // a field a future ticket adds to voice must not be dropped by a build that predates it.
        val withUnknownVoiceField = example().replace(
            "\"lineCount\": 118",
            """"lineCount": 118,
      "voice": { "speakerId": 447, "toneQuality": "gravelly" }""",
        )

        val once = ManifestCodec.decode(withUnknownVoiceField)
        val twice = ManifestCodec.decode(ManifestCodec.encode(once))
        assertEquals(once, twice)

        val voice = once.characters.first { it.id == "sarah" }.voice
        assertEquals(447, voice?.speakerId)
        assertTrue("toneQuality" in voice!!.extras)
    }

    @Test
    fun `an unrecognised voice source degrades to auto`() {
        val imported = example().replace(
            "\"lineCount\": 118",
            """"lineCount": 118,
      "voice": { "source": "imported" }""",
        )

        val manifest = ManifestCodec.decode(imported)
        assertEquals(VoiceSource.AUTO, manifest.characters.first { it.id == "sarah" }.voice?.source)
    }

    @Test
    fun `an out of range confidence is rejected, naming the field path`() {
        val broken = example().replace("\"confidence\": 0.94", "\"confidence\": 1.4")
        val failure = assertFailsWith<ManifestException> { ManifestCodec.decode(broken) }
        assertEquals("characters[0].confidence", failure.path)
        assertTrue("1.4" in failure.message.orEmpty(), "the message should show the value")
    }

    @Test
    fun `an unrecognised gender degrades to unknown and the load succeeds`() {
        val future = example().replace("\"gender\": \"female\"", "\"gender\": \"nonbinary\"")
        val manifest = ManifestCodec.decode(future)
        assertEquals(Gender.UNKNOWN, manifest.characters.first { it.id == "sarah" }.gender)
        // And the raw value is not lost — it is simply not one we can cast from yet.
        assertEquals(Gender.MALE, manifest.characters.first { it.id == "thomas" }.gender)
    }

    @Test
    fun `a duplicate character id is rejected`() {
        val broken = example().replace("\"id\": \"thomas\"", "\"id\": \"sarah\"")
        val failure = assertFailsWith<ManifestException> { ManifestCodec.decode(broken) }
        assertTrue("sarah" in failure.message.orEmpty())
    }

    @Test
    fun `a manifest from a future schema version is refused rather than half read`() {
        val ahead = example().replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")
        val failure = assertFailsWith<ManifestException> { ManifestCodec.decode(ahead) }
        assertEquals("schemaVersion", failure.path)
    }

    @Test
    fun `storage is per book`() {
        val root = File.createTempFile("quire-manifests", "").let {
            it.delete(); it.mkdirs(); it
        }
        try {
            val store = ManifestStore(root)
            val one = ManifestCodec.decode(example())
            val two = one.copy(
                bookId = "other-book",
                characters = listOf(one.characters.first().copy(id = "emma", displayName = "Emma")),
            )
            store.write(one)
            store.write(two)

            assertEquals(
                listOf("sarah", "thomas"),
                store.read(one.bookId)!!.characters.map { it.id },
            )
            assertEquals(listOf("emma"), store.read("other-book")!!.characters.map { it.id })
            assertEquals(listOf("3f1a2b7c9d0e4f58", "other-book"), store.bookIds())
            assertNull(store.read("never-scanned"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `a book id that is not filesystem safe cannot escape the store directory`() {
        val root = File.createTempFile("quire-manifests", "").let { it.delete(); it.mkdirs(); it }
        try {
            val store = ManifestStore(root)
            val nasty = ManifestCodec.decode(example()).copy(bookId = "../../escaped")
            store.write(nasty)
            assertEquals(listOf("../../escaped"), store.bookIds())
            assertEquals(1, root.listFiles()!!.size, "wrote outside the store directory")
        } finally {
            root.deleteRecursively()
        }
    }
}
