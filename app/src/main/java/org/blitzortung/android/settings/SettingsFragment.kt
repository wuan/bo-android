package org.blitzortung.android.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceFragmentCompat
import org.blitzortung.android.app.R

class SettingsFragment : PreferenceFragmentCompat() {
    public override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, true)
    }