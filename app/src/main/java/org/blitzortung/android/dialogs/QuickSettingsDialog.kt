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
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import androidx.core.content.edit

class QuickSettingsDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = Builder(activity)
        val layoutInflater = requireActivity().layoutInflater

        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)

        val regionValues = resources.getStringArray(R.array.regions_values)
        val currentRegionValue = preferences.get(PreferenceKey.REGION, regionValues[0])
        val selectedRegion = getSelectedIndex(regionValues, currentRegionValue)

        val gridSizeValues = resources.getStringArray(R.array.grid_size_values)
        val currentGridSizeValue = preferences.get(PreferenceKey.GRID_SIZE, gridSizeValues[0])
        val selectedGridSize = getSelectedIndex(gridSizeValues, currentGridSizeValue)

        val countThresholdValues = resources.getStringArray(R.array.count_threshold_values)
        val currentCountThresholdValue = preferences.get(PreferenceKey.COUNT_THRESHOLD, countThresholdValues[0])
        val selectedCountThreshold = getSelectedIndex(countThresholdValues, currentCountThresholdValue)

        val queryPeriodValues = resources.getStringArray(R.array.query_period_values)
        val currentQueryPeriodValue = preferences.get(PreferenceKey.QUERY_PERIOD, queryPeriodValues[0])
        val selectedQueryPeriod = getSelectedIndex(queryPeriodValues, currentQueryPeriodValue)

        val intervalDurationValues = resources.getStringArray(R.array.interval_duration_values)
        val currentIntervalDurationValue = preferences.get(PreferenceKey.INTERVAL_DURATION, intervalDurationValues[1])
        val selectedIntervalDuration = getSelectedIndex(intervalDurationValues, currentIntervalDurationValue)

        val historicTimestepValues = resources.getStringArray(R.array.historic_timestep_values)
        val currentHistoricTimestepValue = preferences.get(PreferenceKey.HISTORIC_TIMESTEP, historicTimestepValues[1])
        val selectedHistoricTimestepDuration = getSelectedIndex(historicTimestepValues, currentHistoricTimestepValue)

        val animationIntervalDurationValues = resources.getStringArray(R.array.animation_interval_duration_values)
        val currentAnimationIntervalDurationValue =
            preferences.get(PreferenceKey.ANIMATION_INTERVAL_DURATION, animationIntervalDurationValues[1])
        val selectedAnimationInterval =
            getSelectedIndex(animationIntervalDurationValues, currentAnimationIntervalDurationValue)

        @SuppressLint("InflateParams") val view = layoutInflater.inflate(R.layout.quick_settings_dialog, null, false)

        val selectedRegionList: Spinner = view.findViewById(R.id.selected_region)
        selectedRegionList.setSelection(selectedRegion)

        val gridSizeSpinner: Spinner = view.findViewById(R.id.selected_grid_size)
        gridSizeSpinner.setSelection(selectedGridSize)

        val countThresholdSpinner: Spinner = view.findViewById(R.id.selected_count_threshold)
        countThresholdSpinner.setSelection(selectedCountThreshold)

        val intervalDurationSpinner: Spinner = view.findViewById(R.id.selected_interval_duration)
        intervalDurationSpinner.setSelection(selectedIntervalDuration)

        val queryPeriodSpinner: Spinner = view.findViewById(R.id.selected_query_period)
        queryPeriodSpinner.setSelection(selectedQueryPeriod)

        val historicTimestepSpinner: Spinner = view.findViewById(R.id.selected_historic_timestep)
        historicTimestepSpinner.setSelection(selectedHistoricTimestepDuration)

        val animationIntervalDuration: Spinner = view.findViewById(R.id.selected_animation_interval_durations)
        animationIntervalDuration.setSelection(selectedAnimationInterval)

        builder.setView(view).setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
            val regionValue = regionValues[selectedRegionList.selectedItemPosition]
            val gridSizeValue = gridSizeValues[gridSizeSpinner.selectedItemPosition]
            val countThresholdValue = countThresholdValues[countThresholdSpinner.selectedItemPosition]
            val intervalDurationValue = intervalDurationValues[intervalDurationSpinner.selectedItemPosition]
            val queryPeriodValue = queryPeriodValues[queryPeriodSpinner.selectedItemPosition]
            val animationIntervalValue = animationIntervalDurationValues[animationIntervalDuration.selectedItemPosition]

            preferences.edit {
                putString(PreferenceKey.REGION.toString(), regionValue)
                    .putString(PreferenceKey.GRID_SIZE.toString(), gridSizeValue)
                    .putString(PreferenceKey.COUNT_THRESHOLD.toString(), countThresholdValue)
                    .putString(PreferenceKey.INTERVAL_DURATION.toString(), intervalDurationValue)
                    .putString(PreferenceKey.QUERY_PERIOD.toString(), queryPeriodValue)
                    .putString(PreferenceKey.HISTORIC_TIMESTEP.toString(), currentHistoricTimestepValue)
                    .putString(PreferenceKey.ANIMATION_INTERVAL_DURATION.toString(), animationIntervalValue)
            }
        }.setNegativeButton(R.string.cancel) { _: DialogInterface, _: Int -> }

        return builder.create()
    }

    private fun getSelectedIndex(regionValues: Array<String>, currentRegionValue: String): Int {
        var selectedRegion = 0
        for (regionValue in regionValues) {
            if (regionValue == currentRegionValue) {
                break
            }
            selectedRegion++
        }
        return if (selectedRegion < regionValues.size) selectedRegion else 0
    }
}
