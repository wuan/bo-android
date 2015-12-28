package org.blitzortung.android.alert

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
import org.blitzortung.android.alert.data.AlertContext
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
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
        private val vibrator: Vibrator,
        private val notificationHandler: NotificationHandler,
        private val alertStatusHandler: AlertStatusHandler = AlertStatusHandler(AlertSectorHandler())
) : OnSharedPreferenceChangeListener {
    var alertParameters: AlertParameters

    var alertEventConsumer: ((AlertEvent) -> Unit)? = null
        set (alertEventConsumer: ((AlertEvent) -> Unit)?) {
            field = alertEventConsumer
            updateLocationHandler()
        }

    private var lastStrikes: Collection<Strike>? = null

    private var alertSignal: AlertSignal = AlertSignal()

    private var alertEvent: AlertEvent? = null

    var currentLocation: Location? = null
        private set
    var isAlertEnabled: Boolean = false
        private set
    private var alarmValid: Boolean = false
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
                invalidateAlert()
            }
        } else if (event is ClearDataEvent) {
            invalidateAlert()
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

        alarmValid = false
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
        lastStrikes = strikes

        val location = currentLocation
        if (isAlertEnabled && location != null && strikes != null) {
            val alertContext = AlertContext(location, alertParameters, createSectors())
            alarmValid = true
            alertStatusHandler.checkStrikes(alertContext, strikes, location)
            alertResult = alertStatusHandler.getCurrentActivity(alertContext);
            processResult(alertContext, alertResult)
        } else {
            invalidateAlert()
        }
    }

    private fun createSectors(): List<AlertSector> {
        val sectorLabels = alertParameters.sectorLabels
        val sectorWidth = 360f / sectorLabels.size

        val sectors: MutableList<AlertSector> = arrayListOf()

        var bearing = -180f
        for (sectorLabel in sectorLabels) {
            var minimumSectorBearing = bearing - sectorWidth / 2.0f
            minimumSectorBearing += (if (minimumSectorBearing < -180f) 360f else 0f)
            val maximumSectorBearing = bearing + sectorWidth / 2.0f
            val alertSector = AlertSector(sectorLabel, minimumSectorBearing, maximumSectorBearing, createRanges())
            sectors.add(alertSector)
            bearing += sectorWidth
        }
        return sectors.toList()
    }

    private fun createRanges(): List<AlertSectorRange> {
        val rangeSteps = alertParameters.rangeSteps

        val ranges: MutableList<AlertSectorRange> = arrayListOf()
        var rangeMinimum = 0.0f
        for (rangeMaximum in rangeSteps) {
            val alertSectorRange = AlertSectorRange(rangeMinimum, rangeMaximum)
            ranges.add(alertSectorRange)
            rangeMinimum = rangeMaximum
        }
        return ranges.toList()
    }

    fun getTextMessage(alertContext: AlertContext, notificationDistanceLimit: Float): String {
        return alertStatusHandler.getTextMessage(alertContext, notificationDistanceLimit)
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

    fun invalidateAlert() {
        val previousAlarmValidState = alarmValid
        alarmValid = false

        if (previousAlarmValidState) {
            broadcastClear()
        }
    }

    private fun broadcastClear() {
        alertEventConsumer?.let { consumer ->
            consumer(ALERT_CANCEL_EVENT)
            this.alertEvent = ALERT_CANCEL_EVENT
        }
    }

    private fun broadcastResult(alertContext: AlertContext, alertResult: AlertResult?) {
        alertEventConsumer?.let { consumer ->
            val alertEvent = AlertResultEvent(alertContext, alertResult)
            consumer(alertEvent)
            this.alertEvent = alertEvent
        }
    }

    private fun processResult(alertContext: AlertContext, alertResult: AlertResult?) {
        if (alertResult != null) {

            if (alertResult.closestStrikeDistance <= signalingDistanceLimit) {
                val signalingLatestTimestamp = alertStatusHandler.getLatestTimstampWithin(signalingDistanceLimit, alertContext)
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
                val notificationLatestTimestamp = alertStatusHandler.getLatestTimstampWithin(notificationDistanceLimit, alertContext)
                if (notificationLatestTimestamp > notificationLastTimestamp) {
                    Log.v(Main.LOG_TAG, "AlertHandler.processResult() perform notification")
                    notificationHandler.sendNotification(context.resources.getString(R.string.activity) + ": " + getTextMessage(alertContext, notificationDistanceLimit))
                    notificationLastTimestamp = notificationLatestTimestamp
                } else {
                    Log.d(Main.LOG_TAG, "AlertHandler.processResult() previous signaling event: %d vs %d".format(notificationLatestTimestamp, signalingLastTimestamp))
                }
            } else {
                notificationHandler.clearNotification()
            }
        } else {
            notificationHandler.clearNotification()
        }

        Log.v(Main.LOG_TAG, "AlertHandler.processResult() broadcast result %s".format(alertResult))

        broadcastResult(alertContext, alertResult)
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
