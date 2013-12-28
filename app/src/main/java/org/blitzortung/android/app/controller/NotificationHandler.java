package org.blitzortung.android.app.controller;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;

public class NotificationHandler {

    private final NotificationManager notificationService;
    private final Activity activity;

    public NotificationHandler(Activity activity) {
        notificationService = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
        this.activity = activity;

    }

    public void sendNotification(String notificationText) {
        if (notificationService != null) {
            Notification notification = new Notification(R.drawable.icon, notificationText, System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(activity, 0, new Intent(activity, Main.class), 0);
            notification.setLatestEventInfo(activity, activity.getResources().getText(R.string.app_name), notificationText, contentIntent);

            notificationService.notify(R.id.alarm_notification_id, notification);
        }
    }

    public void clearNotification() {
        if (notificationService != null) {
            notificationService.cancel(R.id.alarm_notification_id);
        }
    }
}
