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
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Spinner
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.jetbrains.anko.defaultSharedPreferences

class QuickSettingsDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = Builder(activity)
        val layoutInflater = activity.layoutInflater

        val preferences = context.defaultSharedPreferences

        val regionValues = resources.getStringArray(R.array.regions_values)
        val currentRegionValue = preferences.get(PreferenceKey.REGION, regionValues[0])
        val selectedRegion = getSelectedIndex(regionValues, currentRegionValue)

        val rasterSizeValues = resources.getStringArray(R.array.raster_size_values)
        val currentRasterSizeValue = preferences.get(PreferenceKey.RASTER_SIZE, rasterSizeValues[1])
        val selectedRasterSize = getSelectedIndex(rasterSizeValues, currentRasterSizeValue)

        val countThresholdValues = resources.getStringArray(R.array.count_threshold_values)
        val currentCountThresholdValue = preferences.get(PreferenceKey.COUNT_THRESHOLD, countThresholdValues[1])
        val selectedCountThreshold = getSelectedIndex(countThresholdValues, currentCountThresholdValue)

        val queryPeriodValues = resources.getStringArray(R.array.query_period_values)
        val currentQueryPeriodValue = preferences.get(PreferenceKey.QUERY_PERIOD, queryPeriodValues[0])
        val selectedQueryPeriod = getSelectedIndex(queryPeriodValues, currentQueryPeriodValue)

        val intervalDurationValues = resources.getStringArray(R.array.interval_duration_values)
        val currentIntervalDurationValue = preferences.get(PreferenceKey.INTERVAL_DURATION, intervalDurationValues[1])
        val selectedIntervalDuration = getSelectedIndex(intervalDurationValues, currentIntervalDurationValue)

        @SuppressLint("InflateParams") val view = layoutInflater.inflate(R.layout.quick_settings_dialog, null, false)

        val selectedRegionList = view.findViewById(R.id.selected_region) as Spinner
        selectedRegionList.setSelection(selectedRegion)

        val rasterSizeSpinner = view.findViewById(R.id.selected_raster_size) as Spinner
        rasterSizeSpinner.setSelection(selectedRasterSize)

        val countThresholdSpinner = view.findViewById(R.id.selected_count_threshold) as Spinner
        countThresholdSpinner.setSelection(selectedCountThreshold)

        val intervalDurationSpinner = view.findViewById(R.id.selected_interval_duration) as Spinner
        intervalDurationSpinner.setSelection(selectedIntervalDuration)

        val queryPeriodSpinner = view.findViewById(R.id.selected_query_period) as Spinner
        queryPeriodSpinner.setSelection(selectedQueryPeriod)

        builder.setView(view).setPositiveButton(R.string.ok, { dialog: DialogInterface, i: Int ->
            val regionValue = regionValues[selectedRegionList.selectedItemPosition]
            val rasterSizeValue = rasterSizeValues[rasterSizeSpinner.selectedItemPosition]
            val countThresholdValue = countThresholdValues[countThresholdSpinner.selectedItemPosition]
            val intervalDurationValue = intervalDurationValues[intervalDurationSpinner.selectedItemPosition]
            val queryPeriodValue = queryPeriodValues[queryPeriodSpinner.selectedItemPosition]

            preferences.edit()
                    .putString(PreferenceKey.REGION.toString(), regionValue)
                    .putString(PreferenceKey.RASTER_SIZE.toString(), rasterSizeValue)
                    .putString(PreferenceKey.COUNT_THRESHOLD.toString(), countThresholdValue)
                    .putString(PreferenceKey.INTERVAL_DURATION.toString(), intervalDurationValue)
                    .putString(PreferenceKey.QUERY_PERIOD.toString(), queryPeriodValue).apply()
        }).setNegativeButton(R.string.cancel, { dialog: DialogInterface, i: Int -> })

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
