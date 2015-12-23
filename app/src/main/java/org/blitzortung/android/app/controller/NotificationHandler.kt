package org.blitzortung.android.app.controller


import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R

class NotificationHandler(private val context: Context) {

    private val notificationService: NotificationManager?

    init {
        notificationService = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun sendNotification(notificationText: String) {
        if (notificationService != null) {
            val intent = Intent(context, Main::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

            val notificationBuilder = Notification.Builder(context).setSmallIcon(R.drawable.icon).setContentTitle(context.resources.getText(R.string.app_name)).setContentText(notificationText).setContentIntent(contentIntent).setAutoCancel(true)

            notificationService.notify(R.id.alarm_notification_id, notificationBuilder.notification)
        }
    }

    fun clearNotification() {
        notificationService?.cancel(R.id.alarm_notification_id)
    }
}
