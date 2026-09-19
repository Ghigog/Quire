package quire.app.companion

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout

/**
 * QUI-001 scaffold: an empty screen, nothing more.
 *
 * Pure black-on-white per CLAUDE.md §7 — this runs on a Kaleido 3 panel and the module
 * carries no UI toolkit it would otherwise have to justify against the footprint budget.
 * Import, the SLM scan and progress UI are QUI-025's job, not this ticket's.
 */
class MainActivity : Activity() {
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.WHITE) })
    }
}
