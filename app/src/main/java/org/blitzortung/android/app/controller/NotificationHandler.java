package org.blitzortung.android.app.controller;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
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
            Intent intent = new Intent(context, Main.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            Notification.Builder notificationBuilder =
                    new Notification.Builder(context)
                            .setSmallIcon(R.drawable.icon)
                            .setContentTitle(context.getResources().getText(R.string.app_name))
                            .setContentText(notificationText)
                            .setContentIntent(contentIntent);

            notificationService.notify(R.id.alarm_notification_id, notificationBuilder.getNotification());
        }
    }

    public void clearNotification() {
        if (notificationService != null) {
            notificationService.cancel(R.id.alarm_notification_id);
        }
    }
}
