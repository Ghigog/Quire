package quire.desktop

import java.io.File
import quire.voice.foundry.QualityList
import quire.voice.foundry.SpeakerProfile

/** Silence inserted between rendered segments, so a turn change is audible as a beat. */
internal const val GAP_MS = 180

/**
 * Just enough argument parsing for a handful of flags.
 *
 * `--flag value` and bare `--flag` both work; the first positional argument is the book. A
 * dependency for this would be larger than the thing it configures.
 */
internal class Flags private constructor(
    private val positional: List<String>,
    private val named: Map<String, String>,
) {
    fun get(name: String): String? = named[name]
    fun has(name: String): Boolean = named.containsKey(name)
    fun int(name: String): Int? = named[name]?.toIntOrNull()
    fun double(name: String): Double? = named[name]?.toDoubleOrNull()

    /** The EPUB, which every command takes as its first positional argument. */
    fun book(): File {
        val path = positional.firstOrNull()
            ?: throw IllegalArgumentException("this command needs an EPUB path — see `quire help`")
        return File(path).takeIf { it.isFile }
            ?: throw IllegalArgumentException("no such file: $path")
    }

    companion object {
        fun parse(tokens: List<String>): Flags {
            val positional = mutableListOf<String>()
            val named = mutableMapOf<String, String>()
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                if (token.startsWith("--")) {
                    val name = token.removePrefix("--")
                    val next = tokens.getOrNull(i + 1)
                    if (next != null && !next.startsWith("--")) {
                        named[name] = next
                        i += 2
                    } else {
                        named[name] = "true"
                        i += 1
                    }
                } else {
                    positional += token
                    i += 1
                }
            }
            return Flags(positional, named)
        }
    }
}

/** The committed F0/quality fixtures, found by walking up so `:desktop:run` just works. */
internal fun fixturesDir(): File {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        val candidate = File(dir, "fixtures/voices")
        if (File(candidate, "libritts_r-f0.tsv").isFile) return candidate
        dir = dir.parentFile
    }
    throw IllegalStateException("could not find fixtures/voices — pass --voices-dir")
}

internal fun profilesIn(dir: File): SpeakerProfile =
    SpeakerProfile.parse(requireFile(dir, "libritts_r-f0.tsv").readLines().asSequence())

internal fun qualityIn(dir: File): QualityList =
    QualityList.parse(requireFile(dir, "libritts_r-quality.tsv").readLines().asSequence())

internal fun requireFile(dir: File, name: String): File =
    File(dir, name).takeIf { it.isFile }
        ?: throw IllegalStateException("no $name in ${dir.path} — pass --voices-dir")
