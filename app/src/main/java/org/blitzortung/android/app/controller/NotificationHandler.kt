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
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            createNotificationChannel()
        }
    }

    fun sendNotification(notificationText: String) {
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
                createNotification(contentIntent, notificationText)
            }

            else -> {
                createJellyBeanNotification(contentIntent, notificationText)
            }
        }

        notificationManager.notify(R.id.alarm_notification_id, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(contentIntent: PendingIntent?, notificationText: String): Notification {
        return Notification.Builder(context, CHANNEL_ID)
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

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                setImportance(IMPORTANCE_LOW)
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "channel"
    }

}
