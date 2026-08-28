package quire.spike.tts

import android.content.Context
import android.os.Debug
import java.io.File

/**
 * The QUI-017 measurement: real-time factor, memory and load time per candidate, on the
 * device, against a fixed text.
 */
object Benchmark {

    /** PRD §5's fixture: roughly ten seconds of speech. */
    val FIXTURE = """
        The rain had not let up since morning, and the windows of the reading room were grey
        with it. Sarah crossed to the window and stood there a while without saying anything
        at all. Thomas did not turn from the desk, though he had stopped writing some minutes
        before, and the clock in the hall struck four.
    """.trimIndent().replace(Regex("\\s+"), " ")

    data class Result(
        val candidate: Candidate,
        val loadMs: Long,
        val synthesisMs: Long,
        val audioMs: Long,
        val peakRssMb: Int,
        val diskMb: Int,
        val voices: Int,
        val sampleRate: Int,
    ) {
        val rtf: Double get() = if (audioMs == 0L) 0.0 else synthesisMs.toDouble() / audioMs

        /** PRD §5: RTF ≤ 0.15. */
        val meetsRtf: Boolean get() = rtf <= 0.15

        override fun toString() = buildString {
            appendLine("${candidate.label}")
            appendLine("  load        ${loadMs} ms")
            appendLine("  synthesis   ${synthesisMs} ms for ${audioMs} ms of audio")
            appendLine("  RTF         %.3f  %s".format(rtf, if (meetsRtf) "PASS (<= 0.15)" else "FAIL (> 0.15)"))
            appendLine("  peak RSS    ${peakRssMb} MB")
            appendLine("  on disk     ${diskMb} MB")
            appendLine("  voices      $voices at ${sampleRate} Hz")
        }
    }

    fun run(context: Context, candidate: Candidate, threads: Int): Result? {
        Runtime.getRuntime().gc()
        val loadStart = System.nanoTime()
        val engine = TtsEngine.load(context, candidate, threads) ?: return null
        val loadMs = (System.nanoTime() - loadStart) / 1_000_000

        // A first short synthesis to page the model in, so the measurement is steady-state
        // rather than a report on lazy initialisation.
        engine.synthesise("Ready.", speakerId = 0, speed = 1.0f)

        val start = System.nanoTime()
        val samples = engine.synthesise(FIXTURE, speakerId = 0, speed = 1.0f)
        val synthesisMs = (System.nanoTime() - start) / 1_000_000
        val audioMs = samples.size * 1000L / engine.sampleRate

        val memInfo = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
        val result = Result(
            candidate = candidate,
            loadMs = loadMs,
            synthesisMs = synthesisMs,
            audioMs = audioMs,
            peakRssMb = memInfo.totalPss / 1024,
            diskMb = (dirSize(ModelStore.dirFor(context, candidate)) / (1024 * 1024)).toInt(),
            voices = engine.voiceCount,
            sampleRate = engine.sampleRate,
        )
        engine.release()
        return result
    }

    private fun dirSize(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
