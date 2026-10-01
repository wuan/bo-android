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

package org.blitzortung.android.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog.Builder
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Spinner
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import java.util.Locale
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.settings.putString

class QuickSettingsDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = Builder(activity)
        val layoutInflater = requireActivity().layoutInflater

        @SuppressLint("InflateParams")
        val view = layoutInflater.inflate(R.layout.quick_settings_dialog, null, false)

        val preferences = PreferenceManager.getDefaultSharedPreferences(activity as Context)

        val spinnerManager = SpinnerManager(resources, view, preferences)

        spinnerManager.add(
            R.array.regions_values,
            PreferenceKey.REGION,
            R.id.selected_region,
            defaultIndex = 1,
        )

        spinnerManager.add(
            R.array.grid_size_values,
            PreferenceKey.GRID_SIZE,
            R.id.selected_grid_size,
        )

        spinnerManager.addCheckBox(
            PreferenceKey.SHOW_GRID,
            R.id.selected_show_grid,
            defaultValue = true,
        )

        val supportsClusters =
            preferences
                .get(PreferenceKey.DATA_SOURCE, DataProviderType.RPC.toString())
                .uppercase(Locale.getDefault()) == DataProviderType.RPC.toString()

        spinnerManager
            .addCheckBox(
                PreferenceKey.SHOW_CLUSTERS,
                R.id.selected_show_clusters,
            ).isEnabled = supportsClusters

        spinnerManager.add(
            R.array.query_period_values,
            PreferenceKey.QUERY_PERIOD,
            R.id.selected_query_period,
            defaultIndex = 2,
        )

        spinnerManager.add(
            R.array.interval_duration_values,
            PreferenceKey.INTERVAL_DURATION,
            R.id.selected_interval_duration,
            defaultIndex = 1,
        )

        spinnerManager.add(
            R.array.historic_timestep_values,
            PreferenceKey.HISTORIC_TIMESTEP,
            R.id.selected_historic_timestep,
            defaultIndex = 1,
        )

        spinnerManager.add(
            R.array.animation_interval_duration_values,
            PreferenceKey.ANIMATION_INTERVAL_DURATION,
            R.id.selected_animation_interval_durations,
            defaultIndex = 1,
        )

        return builder.setView(view)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> spinnerManager.updateSettings() }
            .setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int -> }
            .create()
    }
}

class SpinnerManager(
    val resources: Resources,
    val view: View,
    val preferences: SharedPreferences,
) {
    private val entries = mutableListOf<() -> Unit>()

    fun add(
        valuesId: Int,
        preferenceKey: PreferenceKey,
        viewId: Int,
        defaultIndex: Int = 0,
    ) {
        val values = resources.getStringArray(valuesId)
        val currentValue = preferences.get(preferenceKey, values[defaultIndex])
        val selectedIndex = getSelectedIndex(values, currentValue)
        val spinner: Spinner = view.findViewById(viewId)
        spinner.setSelection(selectedIndex)
        entries.add {
            preferences.edit { putString(preferenceKey, values[spinner.selectedItemPosition]) }
        }
    }

    fun addCheckBox(
        preferenceKey: PreferenceKey,
        viewId: Int,
        defaultValue: Boolean = false,
    ): CheckBox {
        val checkBox: CheckBox = view.findViewById(viewId)
        checkBox.isChecked = preferences.get(preferenceKey, defaultValue)
        entries.add {
            preferences.edit { putBoolean(preferenceKey.key, checkBox.isChecked) }
        }
        return checkBox
    }

    private fun getSelectedIndex(
        values: Array<String>,
        currentValue: String,
    ): Int {
        var selectedIndex = 0
        for (regionValue in values) {
            if (regionValue == currentValue) {
                break
            }
            selectedIndex++
        }
        return if (selectedIndex < values.size) selectedIndex else 0
    }

    fun updateSettings() {
        entries.forEach { it() }
    }
}
