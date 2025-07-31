package org.blitzortung.android.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.blitzortung.android.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    public override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}