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
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi

import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.util.isAtLeast

open class NotificationHandler(private val context: Context) {

    private val notificationService: NotificationManager?

    init {
        notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun sendNotification(notificationText: String) {
        if (notificationService != null) {
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
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun createNotification(contentIntent: PendingIntent?, notificationText: String): Notification? {
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

    private fun createLegacyNotification(contentIntent: PendingIntent?, notificationText: String): Notification {
        val notification = Notification(R.drawable.icon, notificationText, System.currentTimeMillis())
        val setLatestEventInfo = Notification::class.java.getDeclaredMethod("setLatestEventInfo", Context::class.java, CharSequence::class.java, CharSequence::class.java, PendingIntent::class.java)
        setLatestEventInfo.invoke(notification, context, context.resources.getText(R.string.app_name), notificationText, contentIntent)
        return notification
    }

}
