package quire.spike.tts

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.net.URL

/**
 * The TTS candidates QUI-017 compares, and the machinery to get them onto the device.
 *
 * Weights are downloaded, never committed and never shipped in the APK (CLAUDE.md §6).
 * Sizes are the compressed download; note them against PRD §5's 450 MB app footprint,
 * which full-precision Kokoro very nearly exhausts on its own.
 */
data class Candidate(
    val id: String,
    val label: String,
    /** The `tts-models` release asset, without the .tar.bz2 suffix. */
    val asset: String,
    val downloadBytes: Long,
    val kind: Kind,
) {
    enum class Kind { KOKORO, VITS, KITTEN }

    val url get() = "$RELEASE_BASE/$asset.tar.bz2"

    companion object {
        const val RELEASE_BASE =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

        val all = listOf(
            Candidate(
                "kitten", "Kitten nano (fp16)", "kitten-nano-en-v0_1-fp16",
                26_855_312, Kind.KITTEN,
            ),
            Candidate(
                "piper", "Piper libritts_r (medium, 904 voices)",
                "vits-piper-en_US-libritts_r-medium", 82_038_311, Kind.VITS,
            ),
            // A single-speaker "low" model. Useless for casting, but it answers the
            // question the libritts_r number cannot: is RTF 0.35 the SoC's limit, or this
            // model's size? If this comes in near 0.1 it is the model, and a smaller
            // multi-speaker model would fix us.
            Candidate(
                "alan-low", "Piper alan LOW (speed baseline, 1 voice)",
                "vits-piper-en_GB-alan-low", 66_000_000, Kind.VITS,
            ),
            Candidate(
                "vctk", "VITS VCTK (109 voices)", "vits-vctk", 151_000_000, Kind.VITS,
            ),
            // Multi-speaker at the same "medium" tier as libritts_r but a different
            // corpus. Tests whether RTF 0.35 is inherent to medium-quality multi-speaker
            // Piper or specific to libritts_r.
            Candidate(
                "vctk-piper", "Piper VCTK medium (multi-voice speed test)",
                "vits-piper-en_GB-vctk-medium", 79_800_000, Kind.VITS,
            ),
            Candidate(
                "kokoro-int8", "Kokoro multi-lang v1.1 (int8)",
                "kokoro-int8-multi-lang-v1_1", 147_031_220, Kind.KOKORO,
            ),
            Candidate(
                "kokoro", "Kokoro en v0.19 (fp32)", "kokoro-en-v0_19",
                319_625_534, Kind.KOKORO,
            ),
        )

        fun byId(id: String) = all.firstOrNull { it.id == id }
    }
}

object ModelStore {

    private const val TAG = "QuireProbe"

    fun root(context: Context) = File(context.filesDir, "models").apply { mkdirs() }

    fun dirFor(context: Context, candidate: Candidate) = File(root(context), candidate.asset)

    fun isInstalled(context: Context, candidate: Candidate) =
        dirFor(context, candidate).let { it.isDirectory && it.list()?.isNotEmpty() == true }

    fun installed(context: Context) = Candidate.all.filter { isInstalled(context, it) }

    /**
     * Download and unpack a candidate, reporting progress as a percentage.
     *
     * Streams the archive straight through bzip2 and tar rather than staging it on disk:
     * the fp32 Kokoro download is 304 MB and there is no reason to hold both the archive
     * and its contents at once.
     */
    fun install(context: Context, candidate: Candidate, onProgress: (Int) -> Unit) {
        val target = dirFor(context, candidate)
        if (target.exists()) target.deleteRecursively()
        val staging = File(root(context), "${candidate.asset}.partial")
        if (staging.exists()) staging.deleteRecursively()
        staging.mkdirs()

        var read = 0L
        var lastReported = -1
        URL(candidate.url).openStream().use { raw ->
            val counting = object : java.io.FilterInputStream(BufferedInputStream(raw, 1 shl 16)) {
                override fun read(b: ByteArray, off: Int, len: Int): Int =
                    super.read(b, off, len).also {
                        if (it > 0) {
                            read += it
                            val pct = ((read * 100) / candidate.downloadBytes).toInt().coerceIn(0, 100)
                            if (pct != lastReported) { lastReported = pct; onProgress(pct) }
                        }
                    }
            }
            TarArchiveInputStream(BZip2CompressorInputStream(counting, true)).use { tar ->
                while (true) {
                    val entry = tar.nextEntry ?: break
                    // Archives contain a single top-level directory; flatten it away so
                    // the layout on disk is predictable.
                    val relative = entry.name.substringAfter('/', "")
                    if (relative.isEmpty()) continue
                    val out = File(staging, relative)
                    if (!out.canonicalPath.startsWith(staging.canonicalPath)) {
                        error("archive entry escapes its directory: ${entry.name}")
                    }
                    if (entry.isDirectory) { out.mkdirs(); continue }
                    out.parentFile?.mkdirs()
                    out.outputStream().use { tar.copyTo(it, 1 shl 16) }
                }
            }
        }
        // Rename only once complete, so a killed download never looks installed.
        check(staging.renameTo(target)) { "could not move ${staging.name} into place" }
        Log.i(TAG, "installed ${candidate.id} into ${target.absolutePath}")
    }

    fun uninstall(context: Context, candidate: Candidate) {
        dirFor(context, candidate).deleteRecursively()
    }
}
