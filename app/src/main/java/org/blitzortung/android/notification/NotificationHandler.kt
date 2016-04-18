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

package org.blitzortung.android.notification


import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertHandler

import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.OnSharedPreferenceChangeListener
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.getAndConvert
import org.blitzortung.android.notification.signal.SignalManager
import org.blitzortung.android.util.isAtLeast
import org.jetbrains.anko.notificationManager
import kotlin.properties.Delegates

class NotificationHandler(alertHandler: AlertHandler, sharedPreferences: SharedPreferences, private val context: Context): OnSharedPreferenceChangeListener {

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when(key) {
            PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT -> notificationDistanceLimit = sharedPreferences.get(key, "50").toFloat()
            PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT -> signalingDistanceLimit = sharedPreferences.get(key, "25").toFloat()
            PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME -> signalingThresholdTime = sharedPreferences.get(key, "25").toLong() * 1000 * 60
        }
    }

    private val notificationService: NotificationManager = context.notificationManager
    private val signalManager = SignalManager(context, sharedPreferences)

    private var notificationDistanceLimit: Float by Delegates.notNull()
    private var signalingDistanceLimit: Float by Delegates.notNull()

    private var signalingThresholdTime: Long = 0
    private var latestSignalingTime: Long = 0

    private val alertConsumer = { event: AlertEvent ->
        if (event is AlertResultEvent) {
            val result = event.alertResult

            if(result != null) {
                if (result.closestStrikeDistance <= signalingDistanceLimit ) {
                    val currentTime = System.currentTimeMillis()

                    if(currentTime > latestSignalingTime + signalingThresholdTime) {
                        signal()

                        latestSignalingTime = currentTime
                    }
                }

                if(result.closestStrikeDistance < notificationDistanceLimit) {
                    sendNotification(context.resources.getString(R.string.activity) + ": " + getTextMessage(result, notificationDistanceLimit))
                } else {
                    clearNotification()
                }
            }
        }
    }

    private fun getTextMessage(alertResult: AlertResult, notificationDistanceLimit: Float): String {
        return alertResult.sectorsByDistance
                .filter { it.key <= notificationDistanceLimit }
                .map {
                    "%s %.0f%s".format(it.value.label, it.key, alertResult.parameters.measurementSystem.unitName)
                }.joinToString()
    }

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferencesChanged(sharedPreferences,
                PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT,
                PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME)

        alertHandler.requestUpdates(alertConsumer)
    }


    fun sendNotification(notificationText: String) {
        val intent = Intent(context, Main::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val notification = if (isAtLeast(Build.VERSION_CODES.JELLY_BEAN)) {
            createNotification(contentIntent, notificationText)
        } else {
            createLegacyNotification(contentIntent, notificationText)
        }

        notificationService.notify(R.id.alarm_notification_id, notification)
    }

    fun signal() {
        signalManager.signal()
    }

    private fun createNotification(contentIntent: PendingIntent?, notificationText: String): Notification? {
        return Notification.Builder(context)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(context.resources.getText(R.string.app_name))
                .setContentText(notificationText)
                .setContentIntent(contentIntent)
                .setAutoCancel(true).build()
    }

    private fun createLegacyNotification(contentIntent: PendingIntent?, notificationText: String): Notification {
        val notification = Notification(R.drawable.icon, notificationText, System.currentTimeMillis())
        val setLatestEventInfo = Notification::class.java.getDeclaredMethod("setLatestEventInfo", Context::class.java, CharSequence::class.java, CharSequence::class.java, PendingIntent::class.java)
        setLatestEventInfo.invoke(notification, context, context.resources.getText(R.string.app_name), notificationText, contentIntent)
        return notification
    }

    fun clearNotification() {
        notificationService.cancel(R.id.alarm_notification_id)
    }
}