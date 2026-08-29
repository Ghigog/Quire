package quire.spike.tts

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/**
 * The QUI-017 bake-off console.
 *
 * Deliberately plain views and no theme: this runs on a Kaleido 3 panel, so it is pure
 * black on white with no animation (CLAUDE.md §7), and the spike carries no UI toolkit it
 * would otherwise have to justify against the footprint budget.
 */
class MainActivity : Activity() {

    private val work = Executors.newSingleThreadExecutor()
    private lateinit var log: TextView
    private lateinit var rows: LinearLayout

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
        }
        root.addView(heading("Quire TTS bake-off — QUI-017"))
        // Which build this is. The APK filename carries the same stamp, but it does not
        // survive installation, and a device with four probes on it needs to be sure.
        root.addView(body("build ${BuildConfig.BUILD_STAMP}"))
        root.addView(
            body(
                "Download a candidate, select it, and NeoReader's Read Aloud speaks with it " +
                    "instead of beeping. Benchmark measures RTF against the 0.15 budget.",
            ),
        )

        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(rows)

        root.addView(button("Beep mode (no model)") {
            Prefs.setEngineId(this, null)
            append("selected: beep mode")
            refresh()
        })
        root.addView(body("Inference threads — the 750G has 2 performance cores and 6 efficiency cores:"))
        root.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                listOf(1, 2, 4, 6).forEach { n ->
                    addView(button(if (Prefs.threads(this@MainActivity) == n) "[$n]" else "$n") {
                        Prefs.setThreads(this@MainActivity, n)
                        append("threads: $n")
                        refresh()
                    })
                }
            },
        )
        root.addView(button("Benchmark everything installed") { benchmarkAll() })

        log = body("").apply { setTextIsSelectable(true) }
        root.addView(ScrollView(this).apply { addView(log) })

        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    override fun onDestroy() {
        work.shutdownNow()
        super.onDestroy()
    }

    private fun refresh() {
        rows.removeAllViews()
        val selected = Prefs.engineId(this)
        for (candidate in Candidate.all) {
            val installed = ModelStore.isInstalled(this, candidate)
            val mark = if (candidate.id == selected) "◆ " else "  "
            val size = "%d MB".format(candidate.downloadBytes / 1024 / 1024)
            rows.addView(body("$mark${candidate.label}  —  $size${if (installed) ", installed" else ""}"))
            rows.addView(
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    if (!installed) {
                        addView(button("Download") { download(candidate) })
                    } else {
                        addView(button("Use this") {
                            Prefs.setEngineId(this@MainActivity, candidate.id)
                            append("selected: ${candidate.label}")
                            refresh()
                        })
                        addView(button("Benchmark") { benchmark(candidate) })
                        addView(button("Delete") {
                            ModelStore.uninstall(this@MainActivity, candidate)
                            if (Prefs.engineId(this@MainActivity) == candidate.id) {
                                Prefs.setEngineId(this@MainActivity, null)
                            }
                            refresh()
                        })
                    }
                },
            )
        }
    }

    private fun download(candidate: Candidate) {
        append("downloading ${candidate.label} …")
        work.execute {
            runCatching {
                var last = -10
                ModelStore.install(this, candidate) { pct ->
                    if (pct >= last + 10) { last = pct; runOnUiThread { append("  $pct%") } }
                }
            }.onSuccess {
                runOnUiThread { append("installed ${candidate.label}"); refresh() }
            }.onFailure {
                runOnUiThread { append("FAILED: ${it.message}") }
            }
        }
    }

    private fun benchmark(candidate: Candidate) {
        append("benchmarking ${candidate.label} …")
        work.execute {
            val result = runCatching { Benchmark.run(this, candidate, Prefs.threads(this)) }
                .onFailure { runOnUiThread { append("FAILED: ${it.message}") } }
                .getOrNull()
            runOnUiThread { append(result?.toString() ?: "could not load ${candidate.id}") }
        }
    }

    private fun benchmarkAll() {
        val installed = ModelStore.installed(this)
        if (installed.isEmpty()) { append("nothing installed yet"); return }
        installed.forEach { benchmark(it) }
    }

    private fun append(line: String) {
        log.text = "${log.text}\n$line"
    }

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.BLACK)
        setPadding(0, 0, 0, 16)
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(Color.BLACK)
        setPadding(0, 8, 0, 8)
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.BLACK)
        setBackgroundColor(Color.WHITE)
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        setOnClickListener { onClick() }
    }
}
