package quire.spike.tts

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * QUI-016: the sustained-power *harness*, not the measurement.
 *
 * [Benchmark] answers a ten-second burst; PRD §5's SLA is drain "per hour of continuous
 * playback", and a burst cannot stand in for that. ADR-0002 §1 is the reason why: at
 * RTF 0.354 the engine synthesises a page in a third of the time it takes to play, then
 * goes quiet until the next page is due (ADR-0004). Battery drain over an hour of real
 * reading is a duty cycle, not one long burst of CPU — so this reproduces that cycle for
 * close to an hour, on the device, with the engine loaded once and kept warm throughout,
 * and leaves the actual drain to be read off the device (docs and procedure in tickets.md
 * QUI-016's Worklog).
 */
object SustainedRun {

    /** One per-minute checkpoint. */
    data class Sample(
        val minute: Int,
        val batteryPct: Int,
        val charging: Boolean,
        val chunks: Int,
        val cumSynthMs: Long,
        val cumAudioMs: Long,
    ) {
        val rtf: Double get() = if (cumAudioMs == 0L) 0.0 else cumSynthMs.toDouble() / cumAudioMs
    }

    class Report {
        val samples = mutableListOf<Sample>()
        var startBatteryPct = -1
        var endBatteryPct = -1
        var refusedCharging = false
        var abortedCharging = false
        var minutesRun = 0.0
        var logPath: String? = null

        fun summary(): String = buildString {
            if (refusedCharging) {
                appendLine("REFUSED: device was on charge at the start — unplug it and retry.")
                return@buildString
            }
            val minutesStr = "%.1f".format(minutesRun)
            appendLine("sustained run — $minutesStr min")
            if (abortedCharging) {
                appendLine("  ABORTED: charger connected mid-run — this reading is void, retry")
            }
            appendLine("  battery   $startBatteryPct% -> $endBatteryPct%")
            val drainedPts = startBatteryPct - endBatteryPct
            val perHour = if (minutesRun > 0) drainedPts * 60.0 / minutesRun else 0.0
            val perHourStr = "%.2f".format(perHour)
            val meets = perHour <= 8.0 && !abortedCharging
            val verdict = if (meets) "PASS (<= 8%/hr)" else "FAIL (> 8%/hr)"
            appendLine("  drain     $drainedPts pts / $minutesStr min  =>  $perHourStr%/hr  $verdict")
            if (!abortedCharging && minutesRun < 55.0) {
                appendLine("  NOTE: stopped short of 60 min — this is an extrapolation, not a full-hour reading")
            }
            logPath?.let { appendLine("  full per-minute trace: $it") }
        }
    }

    @Volatile private var stopRequested = false

    /** Ask a run in progress to wind down at the next chunk boundary. */
    fun requestStop() {
        stopRequested = true
    }

    /**
     * Run [durationMinutes] of simulated playback against [candidate], engine loaded once
     * and kept warm. Returns null only when the engine itself would not load — a refused
     * or aborted run still returns its [Report] so the caller can show why.
     */
    fun run(
        context: Context,
        candidate: Candidate,
        threads: Int,
        durationMinutes: Int = 60,
        onProgress: (String) -> Unit,
    ): Report? {
        stopRequested = false
        val report = Report()

        val (startPct, startCharging) = batteryStatus(context)
        report.startBatteryPct = startPct
        if (startCharging) {
            report.refusedCharging = true
            return report
        }

        onProgress("loading ${candidate.label} …")
        val engine = TtsEngine.load(context, candidate, threads) ?: return null
        // Page the model in once, same as Benchmark, so the loop below measures steady
        // state rather than first-call initialisation.
        engine.synthesise("Ready.", speakerId = 0, speed = 1.0f)

        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quire:sustained-run")
        val log = SustainedLog(context)
        report.logPath = log.description

        try {
            // A ceiling a few minutes past the target, not the target itself: a wake lock
            // that expired mid-chunk would let the device sleep under us and understate
            // the very thing being measured.
            wakeLock.acquire(durationMinutes * 60_000L + 5 * 60_000L)

            val durationMs = durationMinutes * 60_000L
            val started = SystemClock.elapsedRealtime()
            var lastSampleMinute = -1
            var chunks = 0
            var cumSynthMs = 0L
            var cumAudioMs = 0L

            while (SystemClock.elapsedRealtime() - started < durationMs && !stopRequested) {
                val (_, chargingNow) = batteryStatus(context)
                if (chargingNow) {
                    report.abortedCharging = true
                    onProgress("ABORTED: charger connected — this reading is void")
                    break
                }

                val t0 = System.nanoTime()
                val samples = engine.synthesise(Benchmark.FIXTURE, speakerId = 0, speed = 1.0f)
                val synthMs = (System.nanoTime() - t0) / 1_000_000
                val audioMs = samples.size * 1000L / engine.sampleRate
                chunks++
                cumSynthMs += synthMs
                cumAudioMs += audioMs

                val elapsedMinutes = ((SystemClock.elapsedRealtime() - started) / 60_000L).toInt()
                if (elapsedMinutes != lastSampleMinute) {
                    lastSampleMinute = elapsedMinutes
                    val (pct, charging) = batteryStatus(context)
                    val sample = Sample(elapsedMinutes, pct, charging, chunks, cumSynthMs, cumAudioMs)
                    report.samples += sample
                    onProgress(
                        "min=%d battery=%d%% chunks=%d rtf=%.3f"
                            .format(sample.minute, sample.batteryPct, sample.chunks, sample.rtf),
                    )
                    log.append(sample)
                }

                // The duty cycle production sees (ADR-0002 §1, ADR-0004): a page synthesised
                // faster than real time is followed by quiet until the next page is due.
                // Capped to what's left of the target, so the loop ends close to it rather
                // than overrunning by one page's worth of idle.
                val remainingMs = durationMs - (SystemClock.elapsedRealtime() - started)
                val idleMs = minOf(audioMs - synthMs, remainingMs)
                if (idleMs > 0) sleepInterruptibly(idleMs)
            }

            val (endPct, _) = batteryStatus(context)
            report.endBatteryPct = endPct
            report.minutesRun = (SystemClock.elapsedRealtime() - started) / 60_000.0
            return report
        } finally {
            engine.release()
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun sleepInterruptibly(ms: Long) {
        val until = SystemClock.elapsedRealtime() + ms
        while (!stopRequested) {
            val remaining = until - SystemClock.elapsedRealtime()
            if (remaining <= 0) return
            Thread.sleep(minOf(500L, remaining))
        }
    }

    /**
     * Battery percentage and charging state, read the way that has worked since API 1: the
     * sticky [Intent.ACTION_BATTERY_CHANGED] broadcast, fetched with a null receiver rather
     * than an actual registration. `BatteryManager.BATTERY_PROPERTY_STATUS` would be simpler
     * but is API 28+; this probe's minSdk is 26.
     */
    private fun batteryStatus(context: Context): Pair<Int, Boolean> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return pct to charging
    }

    /**
     * Per-minute trace, written the same way [QuireProbeService] writes its log: a private
     * copy under the app's own directory, and best-effort a public copy in Downloads so it
     * can be pulled off the device without a cable. Rewritten whole on each row rather than
     * appended — `openOutputStream(uri, "wa")` does not reliably append through MediaStore
     * on the reference device (see [QuireProbeService]'s own note); a few dozen rows makes
     * that cost nothing.
     */
    private class SustainedLog(private val context: Context) {
        private val header = listOf(
            "minute", "batteryPct", "charging", "chunks", "cumSynthMs", "cumAudioMs", "rtf",
        ).joinToString("\t") + "\n"
        private val rows = StringBuilder(header)
        private val name = "quire-sustained-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.tsv"
        private var downloadsUri: Uri? = null

        val description: String get() = "Downloads/$name"

        fun append(sample: Sample) {
            rows.append(
                listOf(
                    sample.minute, sample.batteryPct, sample.charging, sample.chunks,
                    sample.cumSynthMs, sample.cumAudioMs, "%.3f".format(sample.rtf),
                ).joinToString("\t") + "\n",
            )
            runCatching { context.getExternalFilesDir(null)?.let { java.io.File(it, name).writeText(rows.toString()) } }
            runCatching {
                downloadsFile()?.let { uri ->
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(rows.toString().toByteArray()) }
                }
            }
        }

        private fun downloadsFile(): Uri? {
            downloadsUri?.let { return it }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            return runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/tab-separated-values")
                }
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            }.getOrNull()?.also { downloadsUri = it }
        }
    }
}
