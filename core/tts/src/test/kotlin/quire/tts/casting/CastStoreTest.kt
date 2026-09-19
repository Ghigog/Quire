package quire.tts.casting

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CastStoreTest {

    private fun tempRoot(): File =
        File.createTempFile("quire-casts", "").let { it.delete(); it.mkdirs(); it }

    @Test
    fun `a cast round-trips through the store`() {
        val root = tempRoot()
        try {
            val store = CastStore(root)
            val cast = Cast(
                bookId = "book-1", narratorVoiceId = 0,
                voices = mapOf(
                    "Sarah" to VoiceAssignment(3),
                    "Thomas" to VoiceAssignment(7, rate = 1.05, source = AssignmentSource.USER),
                ),
            )
            store.write(cast)

            assertEquals(cast, store.read("book-1"))
            assertNull(store.read("never-cast"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `a book id that is not filesystem safe cannot escape the store directory`() {
        val root = tempRoot()
        try {
            val store = CastStore(root)
            store.write(Cast(bookId = "../../escaped", narratorVoiceId = 0, voices = emptyMap()))
            assertEquals(1, root.listFiles()!!.size, "wrote outside the store directory")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `storage is per book`() {
        val root = tempRoot()
        try {
            val store = CastStore(root)
            store.write(Cast(bookId = "book-1", narratorVoiceId = 0, voices = mapOf("A" to VoiceAssignment(1))))
            store.write(Cast(bookId = "book-2", narratorVoiceId = 1, voices = mapOf("B" to VoiceAssignment(2))))

            assertEquals(listOf("A"), store.read("book-1")!!.voices.keys.toList())
            assertEquals(listOf("B"), store.read("book-2")!!.voices.keys.toList())
        } finally {
            root.deleteRecursively()
        }
    }
}
