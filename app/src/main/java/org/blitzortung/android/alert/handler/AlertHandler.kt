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
import android.location.Location
import android.util.Log
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.event.AlertCancelEvent
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.controller.NotificationHandler
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.protocol.ConsumerContainer
import org.blitzortung.android.protocol.Event
import org.blitzortung.android.util.MeasurementSystem
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AlertHandler @Inject constructor(
        private val locationHandler: LocationHandler,
        preferences: SharedPreferences,
        private val context: Context,
        private val notificationHandler: NotificationHandler,
        private val alertDataHandler: AlertDataHandler,
        private val alertSignal: AlertSignal

) : OnSharedPreferenceChangeListener {

    private var alertParameters: AlertParameters

    private val alertConsumerContainer: ConsumerContainer<AlertEvent> = object : ConsumerContainer<AlertEvent>() {
        override fun addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "AlertHandler: added first alert consumer")
        }

        override fun removedLastConsumer() {
            Log.d(Main.LOG_TAG, "AlertHandler: removed last alert consumer")
        }
    }

    private var lastStrikes: Strikes? = null

    private var alertEnabled: Boolean = false
        private set

    private var notificationDistanceLimit: Float = 0.0f

    private var notificationLastTimestamp: Long = 0

    private var signalingDistanceLimit: Float = 0.0f

    private var signalingThresholdTime: Long = 0

    private var signalingLastTimestamp: Long = 0

    private val locationEventConsumer: (LocationEvent) -> Unit = { event ->
        Log.v(Main.LOG_TAG, "AlertHandler.locationEventConsumer ${event.location}")

        checkStrikes(lastStrikes, event.location)
    }

    init {
        locationHandler.requestUpdates(locationEventConsumer)
    }

    val dataEventConsumer: (Event) -> Unit = { event ->
        if (event is ResultEvent) {
            Log.v(Main.LOG_TAG, "AlertHandler.dataEventConsumer $event")
            if (!event.failed && event.containsRealtimeData() && event.strikes != null) {
                val strikes = Strikes(event.strikes, event.rasterParameters)
                checkStrikes(strikes, locationHandler.location)
            } else {
                if (!event.containsRealtimeData()) {
                    lastStrikes = null
                }
                broadcastResult(null)
            }
        }
    }

    init {
        Log.d(Main.LOG_TAG, "AlertHandler() create $this")
        val rangeSteps = arrayOf(10f, 25f, 50f, 100f, 250f, 500f)
        val alarmInterval = 10 * 60 * 1000L
        val sectorLabels = context.resources.getStringArray(R.array.direction_names)
        alertParameters = AlertParameters(alarmInterval, rangeSteps, sectorLabels, MeasurementSystem.METRIC)

        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED, PreferenceKey.MEASUREMENT_UNIT,
                PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT,
                PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME, PreferenceKey.ALERT_VIBRATION_SIGNAL,
                PreferenceKey.ALERT_SOUND_SIGNAL)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.ALERT_ENABLED -> {
                alertEnabled = sharedPreferences.get(key, false)
                Log.v(Main.LOG_TAG, "AlertHandler.onSharedPreferenceChanged() alertEnabled = $alertEnabled")
            }

            PreferenceKey.MEASUREMENT_UNIT -> {
                val measurementSystemName = sharedPreferences.get(key, MeasurementSystem.METRIC.toString())

                alertParameters = alertParameters.copy(measurementSystem = MeasurementSystem.valueOf(measurementSystemName))
                Log.v(Main.LOG_TAG, "AlertHandler.onSharedPreferenceChanged() measurementSystem = ${alertParameters.measurementSystem}")
            }

            PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT -> {
                notificationDistanceLimit = sharedPreferences.get(key, "50").toFloat()
                Log.v(Main.LOG_TAG, "AlertHandler.onSharedPreferenceChanged() notificationDistanceLimit = $notificationDistanceLimit")
            }

            PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT -> {
                signalingDistanceLimit = sharedPreferences.get(key, "25").toFloat()
                Log.v(Main.LOG_TAG, "AlertHandler.onSharedPreferenceChanged() signalingDistanceLimit = $signalingDistanceLimit")
            }

            PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME -> {
                signalingThresholdTime = sharedPreferences.get(key, "25").toLong() * 1000 * 60
                Log.v(Main.LOG_TAG, "AlertHandler.onSharedPreferenceChanged() signalingThresholdTime = $signalingThresholdTime")
            }
        }
    }

    private fun checkStrikes(strikes: Strikes?, location: Location?) {
        lastStrikes = strikes

        Log.v(Main.LOG_TAG, "AlertHandler.checkStrikes() strikes: ${strikes != null}, location: ${locationHandler.location != null}")
        val alertResult = if (alertEnabled && location != null && strikes != null) {
            alertDataHandler.checkStrikes(strikes, location, alertParameters)
        } else {
            null
        }

        processResult(alertResult)
    }

    val maxDistance: Float
        get() = alertParameters.rangeSteps.last()

    private fun broadcastResult(alertResult: AlertResult?) {
        val alertEvent = if (alertResult != null) AlertResultEvent(alertResult) else ALERT_CANCEL_EVENT
        Log.v(Main.LOG_TAG, "AlertHandler.broadcastResult(${alertEvent})")
        alertConsumerContainer.storeAndBroadcast(alertEvent)
    }

    private fun processResult(alertResult: AlertResult?) {
        if (alertResult != null) {
            if (alertResult.closestStrikeDistance <= signalingDistanceLimit) {
                alertSignal(alertResult)
            }
            if (alertResult.closestStrikeDistance <= notificationDistanceLimit) {
                alertNotification(alertResult)
            }
        }
        broadcastResult(alertResult)
    }

    private fun alertSignal(alertResult: AlertResult) {
        val signalingLatestTimestamp = alertDataHandler.getLatestTimstampWithin(signalingDistanceLimit, alertResult)
        if (signalingLatestTimestamp > signalingLastTimestamp + signalingThresholdTime) {
            Log.d(Main.LOG_TAG, "AlertHandler.alertSignal() signal ${signalingLatestTimestamp / 1000}")
            alertSignal.emitSignal()
            signalingLastTimestamp = signalingLatestTimestamp
        } else {
            Log.d(Main.LOG_TAG, "AlertHandler.alertSignal() skipped - ${(signalingLatestTimestamp - signalingLastTimestamp) / 1000}, threshold: ${signalingThresholdTime / 1000}")
        }
    }

    private fun alertNotification(alertResult: AlertResult) {
        val notificationLatestTimestamp = alertDataHandler.getLatestTimstampWithin(notificationDistanceLimit, alertResult)
        if (notificationLatestTimestamp > notificationLastTimestamp) {
            Log.d(Main.LOG_TAG, "AlertHandler.alertNotification() notification ${notificationLatestTimestamp / 1000}")
            notificationHandler.sendNotification(context.resources.getString(R.string.activity) + ": " + alertDataHandler.getTextMessage(alertResult, notificationDistanceLimit, context.resources))
            notificationLastTimestamp = notificationLatestTimestamp
        } else {
            Log.d(Main.LOG_TAG, "AlertHandler.alertNotification() skipped ${notificationLatestTimestamp - notificationLastTimestamp}")
        }
    }

    fun requestUpdates(alertEventConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.addConsumer(alertEventConsumer)
    }

    fun removeUpdates(alertEventConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.removeConsumer(alertEventConsumer)
    }

    val alertEvent: AlertEvent
        get() = alertConsumerContainer.currentPayload ?: ALERT_CANCEL_EVENT

    companion object {
        val ALERT_CANCEL_EVENT = AlertCancelEvent()
    }
}

data class Strikes(
        val strikes: Collection<Strike>,
        val rasterParameters: RasterParameters? = null
)
