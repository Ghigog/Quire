package quire.app.companion.voice

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists [CloudVoiceSettings] in `EncryptedSharedPreferences`, backed by a key the Android
 * Keystore holds — never a plain preferences file (ADR-0010: "a key is a secret and the logs
 * are not"). Never logs a value it reads or writes.
 */
class CloudVoiceSettingsStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        FILE_NAME,
        MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun read(): CloudVoiceSettings = CloudVoiceSettings(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        endpoint = prefs.getString(KEY_ENDPOINT, "").orEmpty(),
        apiKey = prefs.getString(KEY_API_KEY, "").orEmpty(),
        voiceId = prefs.getString(KEY_VOICE_ID, "").orEmpty(),
        pricePerThousandChars = prefs.getFloat(KEY_PRICE, 0f).toDouble(),
    )

    fun write(settings: CloudVoiceSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putString(KEY_ENDPOINT, settings.endpoint)
            .putString(KEY_API_KEY, settings.apiKey)
            .putString(KEY_VOICE_ID, settings.voiceId)
            .putFloat(KEY_PRICE, settings.pricePerThousandChars.toFloat())
            .apply()
    }

    private companion object {
        const val FILE_NAME = "cloud_voice_settings"
        const val KEY_ENABLED = "enabled"
        const val KEY_ENDPOINT = "endpoint"
        const val KEY_API_KEY = "api_key"
        const val KEY_VOICE_ID = "voice_id"
        const val KEY_PRICE = "price_per_1000_chars"
    }
}
