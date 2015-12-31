package org.blitzortung.android.alert.handler

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Vibrator
import android.util.Log
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.handler.ProcessingAlertSector
import org.blitzortung.android.alert.handler.ProcessingAlertSectorRange
import org.blitzortung.android.alert.data.AlertSignal
import org.blitzortung.android.alert.event.AlertCancelEvent
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertSectorHandler
import org.blitzortung.android.alert.handler.AlertStatusHandler
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.controller.NotificationHandler
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.result.ClearDataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.protocol.Event
import org.blitzortung.android.util.MeasurementSystem


class AlertHandler(
        private val locationHandler: LocationHandler,
        preferences: SharedPreferences,
        private val context: Context,
        private val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator,
        private val notificationHandler: NotificationHandler = NotificationHandler(context),
        private val alertDataHandler: AlertDataHandler = AlertDataHandler()

) : OnSharedPreferenceChangeListener {
    var alertParameters: AlertParameters

    var alertEventConsumer: ((AlertEvent) -> Unit)? = null
        set (alertEventConsumer: ((AlertEvent) -> Unit)?) {
            field = alertEventConsumer
            updateLocationHandler()
            broadcastResult(alertResult)
        }

    private var lastStrikes: Collection<Strike>? = null

    private var alertSignal: AlertSignal = AlertSignal()

    private var alertEvent: AlertEvent? = null

    var currentLocation: Location? = null
        private set
    var isAlertEnabled: Boolean = false
        private set

    private var notificationDistanceLimit: Float = 0.toFloat()

    private var notificationLastTimestamp: Long = 0

    private var signalingDistanceLimit: Float = 0.toFloat()

    private var signalingLastTimestamp: Long = 0

    val locationEventConsumer: (LocationEvent) -> Unit = { event ->
        Log.v(Main.LOG_TAG, "AlertHandler received location " + currentLocation + " vs " + event.location)
        currentLocation = event.location
        checkStrikes(lastStrikes)
    }

    val dataEventConsumer: (Event) -> Unit = { event ->
        if (event is ResultEvent) {
            if (!event.failed && event.containsRealtimeData()) {
                checkStrikes(event.strikes)
            } else {
                invalidateAndBroadcastAlert()
            }
        } else if (event is ClearDataEvent) {
            invalidateAndBroadcastAlert()
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
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_VIBRATION_SIGNAL)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_SOUND_SIGNAL)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.ALERT_ENABLED -> isAlertEnabled = sharedPreferences.getBoolean(key.toString(), false)

            PreferenceKey.MEASUREMENT_UNIT -> {
                val measurementSystemName = sharedPreferences.getString(key.toString(), MeasurementSystem.METRIC.toString())

                alertParameters = alertParameters.copy(measurementSystem = MeasurementSystem.valueOf(measurementSystemName))
            }

            PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT -> notificationDistanceLimit = java.lang.Float.parseFloat(sharedPreferences.getString(key.toString(), "50"))

            PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT -> signalingDistanceLimit = java.lang.Float.parseFloat(sharedPreferences.getString(key.toString(), "25"))

            PreferenceKey.ALERT_VIBRATION_SIGNAL -> alertSignal = alertSignal.copy(vibrationDuration = sharedPreferences.getInt(key.toString(), 3) * 10)

            PreferenceKey.ALERT_SOUND_SIGNAL -> {
                val signalUri = sharedPreferences.getString(key.toString(), "")
                alertSignal = alertSignal.copy(soundSignal = if (!signalUri.isEmpty()) Uri.parse(signalUri) else null)
            }
        }
    }

    private fun updateLocationHandler() {
        if (isAlertEnabled) {
            alertEventConsumer?.let {
                locationHandler.requestUpdates(locationEventConsumer)
            }
        } else {
            locationHandler.removeUpdates(locationEventConsumer)
            currentLocation = null
            broadcastClear()
        }
    }

    var alertResult: AlertResult? = null

    fun checkStrikes(strikes: Collection<Strike>?) {
        val alertResult = checkStrikes(strikes, currentLocation)
        processResult(alertResult)
        this.alertResult = alertResult
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
        alertEventConsumer = null
        updateLocationHandler()
    }

    val maxDistance: Float
        get() {
            val ranges = alertParameters.rangeSteps
            return ranges[ranges.size - 1]
        }

    fun invalidateAndBroadcastAlert() {
        val previousAlertEvent = alertEvent
        alertEvent = null

        if (previousAlertEvent != null) {
            broadcastClear()
        }
    }

    private fun broadcastClear() {
        alertEventConsumer?.let { consumer ->
            consumer(ALERT_CANCEL_EVENT)
            this.alertEvent = ALERT_CANCEL_EVENT
        }
    }

    private fun broadcastResult(alertResult: AlertResult?) {
        alertEventConsumer?.let { consumer ->
            val alertResultEvent = AlertResultEvent(alertResult)
            Log.v(Main.LOG_TAG, "AlertHandler.broadcastResult() $alertResultEvent -> $consumer")
            consumer(alertResultEvent)
            this.alertEvent = alertResultEvent
        }
    }

    private fun processResult(alertResult: AlertResult?) {
        if (alertResult != null) {
            if (alertResult.closestStrikeDistance <= signalingDistanceLimit) {
                val signalingLatestTimestamp = alertDataHandler.getLatestTimstampWithin(signalingDistanceLimit, alertResult)
                if (signalingLatestTimestamp > signalingLastTimestamp) {
                    Log.v(Main.LOG_TAG, "AlertHandler.processResult() perform alarm")
                    vibrateIfEnabled()
                    playSoundIfEnabled()
                    signalingLastTimestamp = signalingLatestTimestamp
                } else {
                    Log.d(Main.LOG_TAG, "old signaling event: %d vs %d".format(signalingLatestTimestamp, signalingLastTimestamp))
                }
            }

            if (alertResult.closestStrikeDistance <= notificationDistanceLimit) {
                val notificationLatestTimestamp = alertDataHandler.getLatestTimstampWithin(notificationDistanceLimit, alertResult)
                if (notificationLatestTimestamp > notificationLastTimestamp) {
                    Log.v(Main.LOG_TAG, "AlertHandler.processResult() perform notification")
                    notificationHandler.sendNotification(context.resources.getString(R.string.activity) + ": " + alertDataHandler.getTextMessage(alertResult, notificationDistanceLimit))
                    notificationLastTimestamp = notificationLatestTimestamp
                } else {
                    Log.d(Main.LOG_TAG, "AlertHandler.processResult() previous signaling event: %d vs %d".format(notificationLatestTimestamp, signalingLastTimestamp))
                }
            } else {
                notificationHandler.clearNotification()
            }

            Log.v(Main.LOG_TAG, "AlertHandler.processResult() broadcast result %s".format(alertResult))
            broadcastResult(alertResult)
        } else {
            Log.v(Main.LOG_TAG, "AlertHandler.processResult() no result")
            notificationHandler.clearNotification()
            invalidateAndBroadcastAlert()
        }

    }

    private fun vibrateIfEnabled() {
        vibrator.vibrate(alertSignal.vibrationDuration.toLong())
    }

    private fun playSoundIfEnabled() {
        alertSignal.soundSignal?.let { signal ->
            RingtoneManager.getRingtone(context, signal)?.let { ringtone ->
                if (!ringtone.isPlaying) {
                    ringtone.audioAttributes = AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_NOTIFICATION).build()
                    ringtone.play()
                }
                Log.v(Main.LOG_TAG, "playing " + ringtone.getTitle(context))
            }
        }
    }

    fun reconfigureLocationHandler() {
        locationHandler.updateProvider()
    }

    companion object {
        val ALERT_CANCEL_EVENT = AlertCancelEvent()
    }

    fun alertEvent(): AlertEvent? {
        return alertEvent
    }
}
