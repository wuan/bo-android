/*

   Copyright 2015, 2016 Andreas Würl

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

package org.blitzortung.android.app.controller

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import kotlinx.android.synthetic.main.main.*
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.DataChannel
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.data.ParametersController
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.protocol.Event

class HistorySliderController(
        activity: Activity,
        private val preferences: SharedPreferences,
        private val dataHandler: MainDataHandler
) : OnSharedPreferenceChangeListener {
    private var slider: Slider

    private var intervalDuration: Int = 60
    private var historicTimestep: Int = 30

    val dataConsumer = { event: Event ->
        if (event is ResultEvent) {
            Log.v(Main.LOG_TAG, "slider update: ${event.parameters.intervalOffset}")
            activity.timeSlider.value = -event.parameters.intervalOffset.toFloat()
        }
    }

    init {
        slider = activity.timeSlider
        preferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(this.preferences, PreferenceKey.DATA_SOURCE, PreferenceKey.USERNAME, PreferenceKey.PASSWORD, PreferenceKey.RASTER_SIZE, PreferenceKey.COUNT_THRESHOLD, PreferenceKey.REGION, PreferenceKey.INTERVAL_DURATION, PreferenceKey.HISTORIC_TIMESTEP, PreferenceKey.QUERY_PERIOD)

        initializeSlider()
    }

    private fun initializeSlider() {
        slider.addOnSliderTouchListener(object : OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                Log.v(Main.LOG_TAG, "onStartTracking(${slider.value})")
                update(slider.value.toInt())
            }

            override fun onStopTrackingTouch(slider: Slider) {
                Log.v(Main.LOG_TAG, "onStopTracking(${slider.value})")
                update(slider.value.toInt())
            }
        })
    }

    private fun configureSlider() {
        slider.valueFrom = 0.0f
        slider.valueTo = (ParametersController.MAX_HISTORY_RANGE - intervalDuration).toFloat()
        slider.stepSize = historicTimestep.toFloat()
    }

    private fun update(offset: Int) {
        if (dataHandler.invervalOffset(-offset)) {
            updateData()
        }
        if (offset == 0) {
            dataHandler.restart();
        }
    }

    private fun updateData() {
        dataHandler.updateData(setOf(DataChannel.STRIKES))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.INTERVAL_DURATION -> {
                intervalDuration = Integer.parseInt(sharedPreferences.get(key, "60"))
                configureSlider()
            }

            PreferenceKey.HISTORIC_TIMESTEP -> {
                historicTimestep = Integer.parseInt(sharedPreferences.get(key, "30"))
            }

            else -> {
            }
        }
    }

}
