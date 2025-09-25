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

package org.blitzortung.android.app.controller


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.annotation.RequiresApi

import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.util.isAtLeast
import javax.inject.Inject

open class NotificationHandler @Inject constructor(
    private val context: Context,
    private val notificationManager: NotificationManager
) {

    init {
        if (isAtLeast(Build.VERSION_CODES.O)) {
            createNotificationChannels()
        }
    }

    fun sendNotification(notificationText: String, isCloseAlarm: Boolean) { // Added isCloseAlarm parameter
        val intent = Intent(context, Main::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (isAtLeast(Build.VERSION_CODES.M)) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        })
        val contentIntent = PendingIntent.getActivity(context, 0, intent, flags)

        val notification = when {
            isAtLeast(Build.VERSION_CODES.O) -> {
                createNotification(contentIntent, notificationText, isCloseAlarm) // Pass isCloseAlarm
            }

            else -> {
                createJellyBeanNotification(contentIntent, notificationText)
            }
        }

        notificationManager.notify(R.id.alarm_notification_id, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(contentIntent: PendingIntent?, notificationText: String, isCloseAlarm: Boolean): Notification {
        val channelId = if (isCloseAlarm) CLOSE_CHANNEL_ID else DISTANT_CHANNEL_ID
        return Notification.Builder(context, channelId) // Use dynamic channelId
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(context.resources.getText(R.string.app_name))
            .setContentText(notificationText)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true).build()
    }

    private fun createJellyBeanNotification(contentIntent: PendingIntent?, notificationText: String): Notification {
        val builder = Notification.Builder(context)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(context.resources.getText(R.string.app_name))
            .setContentText(notificationText)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        if (isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            builder.setWhen(System.currentTimeMillis())
                .setShowWhen(true)
        }

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() { // Renamed method
        val backgroundName = context.getString(R.string.notification_channel_background_name)
        val backgroundChannel = NotificationChannel(BACKGROUND_CHANNEL_ID, backgroundName, IMPORTANCE_LOW).apply {
            description = context.getString(R.string.notification_channel_background_description)
            enableLights(false)
            enableVibration(false)
            setSound(null, null) // No sound
            // setBypassDnd(false) // Default is false, explicitly state for clarity if desired
        }
        notificationManager.createNotificationChannel(backgroundChannel)

        val distantName = context.getString(R.string.notification_channel_distant_name)
        val distantChannel = NotificationChannel(DISTANT_CHANNEL_ID, distantName, IMPORTANCE_LOW).apply {
            description = context.getString(R.string.notification_channel_distant_description)
            enableLights(false)
            enableVibration(false)
            setSound(null, null) // No sound
            // setBypassDnd(false) // Default is false, explicitly state for clarity if desired
        }
        notificationManager.createNotificationChannel(distantChannel)

        val closeName = context.getString(R.string.notification_channel_close_name)
        val closeChannel = NotificationChannel(CLOSE_CHANNEL_ID, closeName, IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.notification_channel_close_description)
            enableLights(true)
            enableVibration(true)
            // Default sound and vibration will be used for IMPORTANCE_HIGH,
            // or you can set a custom sound if needed.
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
            // setBypassDnd(false) // Default is false, explicitly state for clarity if desired
        }
        notificationManager.createNotificationChannel(closeChannel)
    }

    companion object {
        const val BACKGROUND_CHANNEL_ID = "blitzortung_background_channel"
        const val DISTANT_CHANNEL_ID = "blitzortung_alert_distant_channel"
        const val CLOSE_CHANNEL_ID = "blitzortung_alert_close_channel"
    }

}
