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
import org.blitzortung.android.alert.data.AlertSignal
import org.blitzortung.android.alert.event.AlertCancelEvent
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.controller.NotificationHandler
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.protocol.ConsumerContainer
import org.blitzortung.android.protocol.Event
import org.blitzortung.android.util.MeasurementSystem
import org.blitzortung.android.util.isAtLeast


class AlertHandler(
        private val locationHandler: LocationHandler,
        private val preferences: SharedPreferences,
        private val context: Context,
        private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator,
        private val notificationHandler: NotificationHandler = NotificationHandler(context),
        private val alertDataHandler: AlertDataHandler = AlertDataHandler()

) : OnSharedPreferenceChangeListener {
    var alertParameters: AlertParameters

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

    private val dataHandler = BOApplication.dataHandler

    private var lastStrikes: Collection<Strike>? = null

    private var alertSignal: AlertSignal = AlertSignal()

    var currentLocation: Location? = null
        private set

    var isAlertEnabled: Boolean = false
        private set

    private var notificationDistanceLimit: Float = 0.0f

    private var notificationLastTimestamp: Long = 0

    private var signalingDistanceLimit: Float = 0.0f

    private var signalingThresholdTime: Long = 0

    private var signalingLastTimestamp: Long = 0


    val locationEventConsumer: (LocationEvent) -> Unit = { event ->
        Log.v(Main.LOG_TAG, "AlertHandler: received location ${event.location} (was $currentLocation)")

        this.currentLocation = event.location
        checkStrikes(lastStrikes)
    }

    val dataEventConsumer: (Event) -> Unit = { event ->
        if (event is ResultEvent) {
            Log.v(Main.LOG_TAG, "AlertHandler: received data $event")
            if (!event.failed && event.containsRealtimeData()) {
                checkStrikes(if (event.incrementalData) event.totalStrikes else event.strikes)
            } else {
                broadcastResult(null)
            }
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
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_VIBRATION_SIGNAL)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_SOUND_SIGNAL)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (key) {
            PreferenceKey.ALERT_ENABLED -> isAlertEnabled = sharedPreferences.get(key, false)

            PreferenceKey.MEASUREMENT_UNIT -> {
                val measurementSystemName = sharedPreferences.get(key, MeasurementSystem.METRIC.toString())

                alertParameters = alertParameters.copy(measurementSystem = MeasurementSystem.valueOf(measurementSystemName))
            }

            PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT -> notificationDistanceLimit = sharedPreferences.get(key, "50").toFloat()

            PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT -> signalingDistanceLimit = sharedPreferences.get(key, "25").toFloat()

            PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME -> signalingThresholdTime = sharedPreferences.get(key, "25").toLong() * 1000 * 60

            PreferenceKey.ALERT_VIBRATION_SIGNAL -> alertSignal = alertSignal.copy(vibrationDuration = sharedPreferences.get(key, 3) * 10)

            PreferenceKey.ALERT_SOUND_SIGNAL -> {
                val signalUri = sharedPreferences.get(key, "")
                alertSignal = alertSignal.copy(soundSignal = if (!signalUri.isEmpty()) Uri.parse(signalUri) else null)
            }
        }
    }

    private fun refresh() {
        if (isAlertEnabled) {
            locationHandler.requestUpdates(locationEventConsumer)
            dataHandler.requestInternalUpdates(dataEventConsumer)
        } else {
            locationHandler.removeUpdates(locationEventConsumer)
            dataHandler.removeInternalUpdates(dataEventConsumer)

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
        broadcastEvent(if (alertResult != null) AlertResultEvent(alertResult) else ALERT_CANCEL_EVENT)
    }

    private fun broadcastEvent(alertEvent: AlertEvent) {
        Log.v(Main.LOG_TAG, "AlertHandler.broadcastEvent($alertEvent)")
        alertConsumerContainer.storeAndBroadcast(alertEvent)
    }

    private fun processResult(alertResult: AlertResult?) {
        if (alertResult != null) {
            handleAlert(alertResult)
        } else {
            Log.v(Main.LOG_TAG, "AlertHandler.processResult() no result")
            broadcastResult(null)
        }
    }

    private fun handleAlert(alertResult: AlertResult) {
        if (alertResult.closestStrikeDistance <= signalingDistanceLimit) {
            handleAlertSignal(alertResult)
        }

        if (alertResult.closestStrikeDistance <= notificationDistanceLimit) {
            handleAlertNotification(alertResult)
        }

        Log.v(Main.LOG_TAG, "AlertHandler.handleAlert() broadcast result %s".format(alertResult))
        broadcastResult(alertResult)
    }

    private fun handleAlertSignal(alertResult: AlertResult) {
        val signalingLatestTimestamp = alertDataHandler.getLatestTimstampWithin(signalingDistanceLimit, alertResult)
        if (signalingLatestTimestamp > signalingLastTimestamp + signalingThresholdTime) {
            Log.d(Main.LOG_TAG, "AlertHandler.handleAlertSignal() signal ${signalingLatestTimestamp / 1000}")
            vibrateIfEnabled()
            playSoundIfEnabled()
            signalingLastTimestamp = signalingLatestTimestamp
        } else {
            Log.d(Main.LOG_TAG, "AlertHandler.handleAlertSignal() skipped - ${(signalingLatestTimestamp - signalingLastTimestamp) / 1000}, threshold: ${signalingThresholdTime / 1000}")
        }
    }

    private fun handleAlertNotification(alertResult: AlertResult) {
        val notificationLatestTimestamp = alertDataHandler.getLatestTimstampWithin(notificationDistanceLimit, alertResult)
        if (notificationLatestTimestamp > notificationLastTimestamp) {
            Log.d(Main.LOG_TAG, "AlertHandler.handleAlertNotification() notification ${notificationLatestTimestamp / 1000}")
            notificationHandler.sendNotification(context.resources.getString(R.string.activity) + ": " + alertDataHandler.getTextMessage(alertResult, notificationDistanceLimit))
            notificationLastTimestamp = notificationLatestTimestamp
        } else {
            Log.d(Main.LOG_TAG, "AlertHandler.handleAlertNotification() skipped - ${notificationLatestTimestamp - notificationLastTimestamp}")
        }
    }

    private fun vibrateIfEnabled() {
        vibrator.vibrate(alertSignal.vibrationDuration.toLong())
    }

    private fun playSoundIfEnabled() {
        alertSignal.soundSignal?.let { signal ->
            RingtoneManager.getRingtone(context, signal)?.let { ringtone ->
                if (!ringtone.isPlaying) {
                    if (isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
                        ringtone.audioAttributes = AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
                    } else {
                        ringtone.streamType = AudioManager.STREAM_NOTIFICATION
                    }
                    ringtone.play()
                }
                Log.v(Main.LOG_TAG, "playing " + ringtone.getTitle(context))
            }
        }
    }

    fun requestUpdates(alertEventConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.addConsumer(alertEventConsumer)
    }

    fun removeUpdates(alertEventConsumer: (AlertEvent) -> Unit) {
        alertConsumerContainer.removeConsumer(alertEventConsumer)
    }

    val alertEvent: AlertEvent = alertConsumerContainer.currentPayload ?: ALERT_CANCEL_EVENT

    companion object {
        val ALERT_CANCEL_EVENT = AlertCancelEvent()
    }
}
