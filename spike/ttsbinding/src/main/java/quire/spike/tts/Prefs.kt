package quire.spike.tts

import android.content.Context

/** Which candidate the TTS service should speak with, shared between activity and service. */
object Prefs {
    private const val FILE = "quire-probe"
    private const val KEY_ENGINE = "engine"
    private const val KEY_THREADS = "threads"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** null means beep mode: no model loaded, tones instead of speech. */
    fun engineId(context: Context): String? = prefs(context).getString(KEY_ENGINE, null)

    fun setEngineId(context: Context, id: String?) =
        prefs(context).edit().putString(KEY_ENGINE, id).apply()

    fun threads(context: Context): Int = prefs(context).getInt(KEY_THREADS, 2)

    fun setThreads(context: Context, n: Int) =
        prefs(context).edit().putInt(KEY_THREADS, n).apply()
}
