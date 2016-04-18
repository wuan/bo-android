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

package org.blitzortung.android.alert.handler

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.util.Log
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.event.AlertCancelEvent
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.result.ClearDataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.protocol.ConsumerContainer
import org.blitzortung.android.protocol.Event
import org.blitzortung.android.util.MeasurementSystem


class AlertHandler(
        private val locationHandler: LocationHandler,
        private val dataHandler: DataHandler,
        private val preferences: SharedPreferences,
        private val context: Context,
        private val alertDataHandler: AlertDataHandler = AlertDataHandler()

) : OnSharedPreferenceChangeListener {
    var alertParameters: AlertParameters

    var alertEvent: AlertEvent = ALERT_CANCEL_EVENT
        private set

    val alertConsumerContainer: ConsumerContainer<AlertEvent> = object : ConsumerContainer<AlertEvent>() {
        override fun addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "AlertHandler: added first alert consumer")

            refresh()
        }

        override fun removedLastConsumer() {
            Log.d(Main.LOG_TAG, "AlertHandler: removed last alert consumer")

            refresh()
        }
    }

    private var lastStrikes: Collection<Strike>? = null

    var currentLocation: Location? = null
        private set

    var isAlertEnabled: Boolean = false
        private set

    val locationEventConsumer: (LocationEvent) -> Unit = { event ->
        Log.v(Main.LOG_TAG, "AlertHandler: received location " + currentLocation + " vs " + event.location)
        currentLocation = event.location
        checkStrikes(lastStrikes)
    }

    val dataEventConsumer: (Event) -> Unit = { event ->
        if (event is ResultEvent) {
            if (!event.failed && event.containsRealtimeData()) {
                checkStrikes(if (event.incrementalData) event.totalStrikes else event.strikes)
            } else {
                broadcastResult(null)
            }
        } else if (event is ClearDataEvent) {
            broadcastResult(null)
        }
    }

    init {
        val rangeSteps = arrayOf(10f, 25f, 50f, 100f, 250f, 500f)
        val alarmInterval = 10 * 60 * 1000L
        val sectorLabels = context.resources.getStringArray(R.array.direction_names)
        alertParameters = AlertParameters(alarmInterval, rangeSteps, sectorLabels, MeasurementSystem.METRIC)

        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED)
        onSharedPreferenceChanged(preferences, PreferenceKey.MEASUREMENT_UNIT)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.ALERT_ENABLED -> {
                isAlertEnabled = sharedPreferences.get(key, false)

                //If isAlertEnabled has changed, we  need to refresh the AlertHandler
                refresh()
            }

            PreferenceKey.MEASUREMENT_UNIT -> {
                val measurementSystemName = sharedPreferences.get(key, MeasurementSystem.METRIC.toString())

                alertParameters = alertParameters.copy(measurementSystem = MeasurementSystem.valueOf(measurementSystemName))
            }
        }
    }

    private fun refresh() {
        if (isAlertEnabled) {
            locationHandler.requestUpdates(locationEventConsumer)
            dataHandler.requestUpdates(dataEventConsumer)
        } else {
            locationHandler.removeUpdates(locationEventConsumer)
            dataHandler.removeUpdates(dataEventConsumer)

            currentLocation = null
            broadcastResult(null)
        }
    }

    fun checkStrikes(strikes: Collection<Strike>?) {
        val alertResult = checkStrikes(strikes, currentLocation)
        processResult(alertResult)
    }

    fun checkStrikes(strikes: Collection<Strike>?, location: Location?): AlertResult? {
        lastStrikes = strikes
        return if (isAlertEnabled && location != null && strikes != null) {
            alertDataHandler.checkStrikes(strikes, location, alertParameters)
        } else {
            null
        }
    }

    fun unsetAlertListener() {
        refresh()
    }

    val maxDistance: Float
        get() = alertParameters.rangeSteps.last()

    private fun broadcastResult(alertResult: AlertResult?) {
        alertEvent = if (alertResult != null) AlertResultEvent(alertResult) else ALERT_CANCEL_EVENT
        broadcastEvent()
    }

    private fun broadcastEvent() {
        Log.v(Main.LOG_TAG, "AlertHandler.broadcastResult() $alertEvent")

        alertConsumerContainer.storeAndBroadcast(alertEvent)
    }

    private fun processResult(alertResult: AlertResult?) {
        if (alertResult != null) {
            Log.v(Main.LOG_TAG, "AlertHandler.processResult() broadcast result %s".format(alertResult))
        } else {
            Log.v(Main.LOG_TAG, "AlertHandler.processResult() no result")
        }

        broadcastResult(alertResult)
    }

    fun requestUpdates(alertEventConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.addConsumer(alertEventConsumer)
    }

    fun removeUpdates(alertEventConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.removeConsumer(alertEventConsumer)
    }

    companion object {
        val ALERT_CANCEL_EVENT = AlertCancelEvent()
    }
}
