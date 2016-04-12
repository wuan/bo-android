/*

   Copyright 2016 Andreas Würl

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

import android.content.SharedPreferences
import android.util.Log
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.Parameters
import rx.subjects.BehaviorSubject

class ParametersComponent(
        preferences: SharedPreferences,
        parameters: Parameters
) : OnSharedPreferenceChangeListener {

    var parameters: Parameters
        private set

    private var offsetIncrement: Int = 0

    val observable = BehaviorSubject.create<Parameters>()

    init {
        this.parameters = parameters

        preferences.registerOnSharedPreferenceChangeListener(this)

        listOf(PreferenceKey.RASTER_SIZE, PreferenceKey.COUNT_THRESHOLD, PreferenceKey.REGION, PreferenceKey.INTERVAL_DURATION, PreferenceKey.HISTORIC_TIMESTEP).forEach {
            key ->
            this.parameters = updateParameters(key, preferences)
        }

        if (this.parameters != parameters) {
            trigger()
        }
    }

    fun rewInterval(): Boolean {
        return updateIfChanged(updateInterval(-offsetIncrement))
    }

    fun ffwdInterval(): Boolean {
        return updateIfChanged(updateInterval(offsetIncrement))
    }

    fun goRealtime(): Boolean {
        return updateIfChanged(parameters.copy(intervalOffset = 0))
    }

    private fun updateInterval(offsetIncrement: Int): Parameters {
        var intervalOffset = parameters.intervalOffset + offsetIncrement
        val intervalDuration = parameters.intervalDuration

        if (intervalOffset < -MAX_HISTORICAL_OFFSET + intervalDuration) {
            intervalOffset = -MAX_HISTORICAL_OFFSET + intervalDuration
        } else if (intervalOffset > 0) {
            intervalOffset = 0
        }

        return parameters.copy(intervalOffset = alignValue(intervalOffset))
    }

    private fun alignValue(value: Int): Int {
        return (value / offsetIncrement) * offsetIncrement
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        val updatedParameters = updateParameters(key, sharedPreferences)

        updateIfChanged(updatedParameters)
    }

    private fun updateParameters(key: PreferenceKey, sharedPreferences: SharedPreferences): Parameters {
        val updatedParameters = when (key) {
            PreferenceKey.RASTER_SIZE ->
                parameters.copy(rasterBaselength = Integer.parseInt(sharedPreferences.get(key, "10000")));

            PreferenceKey.COUNT_THRESHOLD ->
                parameters.copy(countThreshold = Integer.parseInt(sharedPreferences.get(key, "1")));

            PreferenceKey.INTERVAL_DURATION ->
                parameters.copy(intervalDuration = Integer.parseInt(sharedPreferences.get(key, "60")));

            PreferenceKey.REGION ->
                parameters.copy(region = Integer.parseInt(sharedPreferences.get(key, "1")));

            PreferenceKey.HISTORIC_TIMESTEP -> {
                offsetIncrement = Integer.parseInt(sharedPreferences.get(key, "30"))
                parameters
            }

            else -> parameters
        }
        return updatedParameters
    }

    private fun updateIfChanged(updatedParameters: Parameters): Boolean {
        if (updatedParameters != parameters) {
            parameters = updatedParameters
            trigger()

            return true
        }
        return false
    }

    companion object {
        private val MAX_HISTORICAL_OFFSET = 24 * 60

    }

    val isRealtime: Boolean
        get() = parameters.isRealtime()

    fun trigger() {
        Log.v(Main.LOG_TAG, "ParametersComponent.trigger() $parameters")
        observable.onNext(parameters)
    }

}