package quire.app.companion.voice

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import quire.app.companion.Library
import quire.app.companion.R
import quire.model.characters.ManifestStore

/**
 * The settings screen ADR-0010 promises: endpoint, key, voice id and a price, off until a
 * reader fills all four in. Opened either from the library (no book) to edit the shared
 * configuration, or from a ready book's row (with [EXTRA_BOOK_ID]) to also show what turning
 * the tier on would cost for that book before the reader commits to it.
 */
class CloudVoiceSettingsActivity : Activity() {

    private lateinit var store: CloudVoiceSettingsStore
    private lateinit var estimateView: TextView
    private lateinit var enabledSwitch: Switch
    private lateinit var endpointField: EditText
    private lateinit var apiKeyField: EditText
    private lateinit var voiceIdField: EditText
    private lateinit var priceField: EditText
    private var bookId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Companion)
        setContentView(R.layout.activity_cloud_voice)
        store = CloudVoiceSettingsStore(this)
        bookId = intent.getStringExtra(EXTRA_BOOK_ID)

        estimateView = findViewById(R.id.cloud_voice_estimate)
        enabledSwitch = findViewById(R.id.cloud_voice_enabled)
        endpointField = findViewById(R.id.cloud_voice_endpoint)
        apiKeyField = findViewById(R.id.cloud_voice_api_key)
        voiceIdField = findViewById(R.id.cloud_voice_id)
        priceField = findViewById(R.id.cloud_voice_price)

        val settings = store.read()
        enabledSwitch.isChecked = settings.enabled
        endpointField.setText(settings.endpoint)
        apiKeyField.setText(settings.apiKey)
        voiceIdField.setText(settings.voiceId)
        priceField.setText(if (settings.pricePerThousandChars > 0) settings.pricePerThousandChars.toString() else "")

        priceField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refreshEstimate()
        })
        refreshEstimate()

        findViewById<Button>(R.id.cloud_voice_save).setOnClickListener { save() }
    }

    private fun refreshEstimate() {
        val id = bookId ?: return
        val manifest = ManifestStore(Library.root(this)).read(id) ?: return
        val price = priceField.text.toString().toDoubleOrNull() ?: 0.0
        val characters = DialogueCostEstimate.estimatedCharacterCount(manifest)
        val cost = DialogueCostEstimate.estimatedCost(manifest, price)
        estimateView.text = getString(R.string.cloud_voice_estimate, characters, "%.2f".format(cost))
        estimateView.visibility = TextView.VISIBLE
    }

    private fun save() {
        store.write(
            CloudVoiceSettings(
                enabled = enabledSwitch.isChecked,
                endpoint = endpointField.text.toString().trim(),
                apiKey = apiKeyField.text.toString(),
                voiceId = voiceIdField.text.toString().trim(),
                pricePerThousandChars = priceField.text.toString().toDoubleOrNull() ?: 0.0,
            ),
        )
        finish()
    }

    companion object {
        const val EXTRA_BOOK_ID = "book_id"
    }
}
