package org.blitzortung.android.app.controller;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;

public class NotificationHandler {

    private final NotificationManager notificationService;
    private final Context context;

    public NotificationHandler(Context context) {
        notificationService = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;

    }

    public void sendNotification(String notificationText) {
        if (notificationService != null) {
            Notification notification = new Notification(R.drawable.icon, notificationText, System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, Main.class), 0);
            notification.setLatestEventInfo(context, context.getResources().getText(R.string.app_name), notificationText, contentIntent);

            notificationService.notify(R.id.alarm_notification_id, notification);
        }
    }

    public void clearNotification() {
        if (notificationService != null) {
            notificationService.cancel(R.id.alarm_notification_id);
        }
    }
}
