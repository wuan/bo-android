/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app

import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import dagger.android.support.AndroidSupportInjection
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.location.LocationHandler
import java.util.Locale
import javax.inject.Inject


class Preferences : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    private val REQUEST_CODE_ALERT_RINGTONE: Int = 123

    @set:Inject
    internal lateinit var preferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        preferences.registerOnSharedPreferenceChangeListener(this)

        configureDataSourcePreferences(preferences)
        configureLocationProviderPreferences(preferences)
        configureOwnLocationSizePreference(preferences)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE -> {
                configureDataSourcePreferences(sharedPreferences)
            }

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

            PreferenceKey.SHOW_LOCATION -> {
                configureOwnLocationSizePreference(sharedPreferences)
            }

            else -> {
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
        if (preference.getKey() == PreferenceKey.ALERT_SOUND_SIGNAL.toString()) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)

            val existingValue: String? = preferences.get(PreferenceKey.ALERT_SOUND_SIGNAL, null)
            if (existingValue != null) {
                if (existingValue.length == 0) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, null as Uri?)
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue))
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
            }

            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
            return true
        } else {
            return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            val ringtone = data.getParcelableExtra<Uri?>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            preferences.edit(commit = true) {
                putString(
                    PreferenceKey.ALERT_SOUND_SIGNAL.toString(),
                    ringtone.toString()
                )
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

fun <T : Preference> PreferenceFragmentCompat.findPreference(key: PreferenceKey): T? {
    return findPreference(key.toString()) as T?
}

fun SharedPreferences.getString(key: PreferenceKey, defValue: String?): String? =
    getString(key.toString(), defValue)

fun SharedPreferences.getInt(key: PreferenceKey, defValue: Int): Int =
    getInt(key.toString(), defValue)

fun SharedPreferences.Editor.putString(key: PreferenceKey, value: String): SharedPreferences.Editor? =
    putString(key.toString(), value)

fun SharedPreferences.Editor.putInt(key: PreferenceKey, value: Int): SharedPreferences.Editor? =
    putInt(key.toString(), value)
