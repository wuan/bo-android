package org.blitzortung.android.settings

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import dagger.android.support.AndroidSupportInjection
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.location.LocationHandler
import java.util.Locale
import javax.inject.Inject

private const val REQUEST_CODE_ALERT_RINGTONE: Int = 123

class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    @set:Inject
    internal lateinit var preferences: SharedPreferences

    private val originalSummaries = mutableMapOf<PreferenceKey, Int>()

    override fun onAttach(context: Context) {
        Log.v(LOG_TAG, "SettingsFragment.onAttach()")
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.v(LOG_TAG, "SettingsFragment.onCreatePreferences()")

        addPreferencesFromResource(R.xml.preferences)

        if (::preferences.isInitialized) {
            preferences.registerOnSharedPreferenceChangeListener(this)

            configureDataSourcePreferences(preferences)
            configureLocationProviderPreferences(preferences)
            configureOwnLocationSizePreference(preferences)

            // Initial summary update for EditTextPreferences
            updateEditTextPreferenceSummaries()
        } else {
            Log.e(LOG_TAG, "SharedPreferences not initialized in SettingsFragment.onCreatePreferences")
        }
    }

    private fun updateEditTextPreferenceSummaries() {
        listOf(
            PreferenceKey.USERNAME,
            PreferenceKey.PASSWORD,
            PreferenceKey.SERVICE_URL,
            PreferenceKey.LOCATION_LONGITUDE,
            PreferenceKey.LOCATION_LATITUDE,
        ).forEach { key ->
            findPreference<EditTextPreference>(key)?.let { updateEditTextPreferenceSummary(it) }
        }
    }

    private fun updateEditTextPreferenceSummary(preference: EditTextPreference) {
        val keyString = preference.key
        val preferenceKey = PreferenceKey.fromString(keyString)

        val currentValue = preference.text
        val originalSummaryResId = originalSummaries[preferenceKey]
        val originalSummary = if (originalSummaryResId != null) getString(originalSummaryResId) else preference.summary?.toString() ?: ""
        val undefinedSummary = getString(R.string.undefined)

        if (preferenceKey == PreferenceKey.PASSWORD) {
            preference.summary = if (!currentValue.isNullOrEmpty()) {
                "********"
            } else {
                originalSummary
            }
        } else if (originalSummary.contains("%s")) {
             preference.summary = String.format(originalSummary, currentValue ?: "")
        }
        else {
            if (!currentValue.isNullOrEmpty()) {
                val summary = if (originalSummary.isEmpty()) "" else "$originalSummary: "
                preference.summary = "$summary$currentValue"
            } else {
                preference.summary = originalSummary.ifEmpty { undefinedSummary }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = listView
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE -> configureDataSourcePreferences(sharedPreferences)
            PreferenceKey.LOCATION_MODE -> {
                val provider = configureLocationProviderPreferences(sharedPreferences)
                val context = this.context
                if (context != null) {
                    if (provider != LocationHandler.MANUAL_PROVIDER &&
                        !(context.getSystemService(LOCATION_SERVICE) as LocationManager).isProviderEnabled(provider)
                    ) {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
            }
            PreferenceKey.SHOW_LOCATION -> configureOwnLocationSizePreference(sharedPreferences)
            PreferenceKey.USERNAME,
            PreferenceKey.PASSWORD,
            PreferenceKey.SERVICE_URL,
            PreferenceKey.LOCATION_LONGITUDE,
            PreferenceKey.LOCATION_LATITUDE -> {
                findPreference<EditTextPreference>(key)?.let { updateEditTextPreferenceSummary(it) }
            }
            else -> {
                // No action needed for other keys
            }
        }
    }

    private fun configureOwnLocationSizePreference(sharedPreferences: SharedPreferences) {
        findPreference<SeekBarPreference>(PreferenceKey.OWN_LOCATION_SIZE.toString())?.isEnabled =
            sharedPreferences.get(PreferenceKey.SHOW_LOCATION, false)
    }

    private fun configureDataSourcePreferences(sharedPreferences: SharedPreferences): DataProviderType {
        val providerTypeString = sharedPreferences.get(PreferenceKey.DATA_SOURCE, DataProviderType.HTTP.toString())
        val providerType = DataProviderType.valueOf(providerTypeString.uppercase(Locale.getDefault()))

        when (providerType) {
            DataProviderType.HTTP -> enableBlitzortungHttpMode()
            DataProviderType.RPC -> enableAppServiceMode()
        }
        return providerType
    }

    private fun configureLocationProviderPreferences(sharedPreferences: SharedPreferences): String {
        val locationProvider = sharedPreferences.get(PreferenceKey.LOCATION_MODE, LocationManager.NETWORK_PROVIDER)
        enableManualLocationMode(locationProvider == LocationHandler.MANUAL_PROVIDER)
        return locationProvider
    }

    private fun enableAppServiceMode() {
        findPreference<ListPreference>(PreferenceKey.GRID_SIZE)?.isEnabled = true
        findPreference<EditTextPreference>(PreferenceKey.SERVICE_URL)?.isEnabled = true
        findPreference<EditTextPreference>(PreferenceKey.USERNAME)?.isEnabled = false
        findPreference<EditTextPreference>(PreferenceKey.PASSWORD)?.isEnabled = false
    }

    private fun enableBlitzortungHttpMode() {
        findPreference<ListPreference>(PreferenceKey.GRID_SIZE)?.isEnabled = false
        findPreference<EditTextPreference>(PreferenceKey.SERVICE_URL)?.isEnabled = false
        findPreference<EditTextPreference>(PreferenceKey.USERNAME)?.isEnabled = true
        findPreference<EditTextPreference>(PreferenceKey.PASSWORD)?.isEnabled = true
    }

    private fun enableManualLocationMode(enabled: Boolean) {
        findPreference<EditTextPreference>(PreferenceKey.LOCATION_LONGITUDE)?.isEnabled = enabled
        findPreference<EditTextPreference>(PreferenceKey.LOCATION_LATITUDE)?.isEnabled = enabled
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == PreferenceKey.ALERT_SOUND_SIGNAL.toString()) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)

            val existingValue: String? = if (::preferences.isInitialized) {
                preferences.get(PreferenceKey.ALERT_SOUND_SIGNAL, null)
            } else {
                Log.e(LOG_TAG, "SharedPreferences not initialized in onPreferenceTreeClick")
                null
            }

            if (existingValue != null) {
                if (existingValue.isEmpty()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue))
                }
            } else {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            }

            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
            return true
        } else {
            return super.onPreferenceTreeClick(preference)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            val ringtone = data.getParcelableExtra<Uri?>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (::preferences.isInitialized) {
                preferences.edit(commit = true) {
                    putString(
                        PreferenceKey.ALERT_SOUND_SIGNAL.toString(),
                        ringtone?.toString() ?: ""
                    )
                }
            } else {
                Log.e(LOG_TAG, "SharedPreferences not initialized in onActivityResult")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

fun <T : Preference> PreferenceFragmentCompat.findPreference(key: PreferenceKey): T? {
    return findPreference<T>(key.toString())
}

fun SharedPreferences.getString(key: PreferenceKey, defValue: String?): String? =
    getString(key.toString(), defValue)

fun SharedPreferences.getInt(key: PreferenceKey, defValue: Int): Int =
    getInt(key.toString(), defValue)

fun SharedPreferences.Editor.putString(key: PreferenceKey, value: String?): SharedPreferences.Editor =
    putString(key.toString(), value)

fun SharedPreferences.Editor.putInt(key: PreferenceKey, value: Int): SharedPreferences.Editor =
    putInt(key.toString(), value)
