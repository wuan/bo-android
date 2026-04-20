package org.blitzortung.android.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WidgetClickReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WIDGET_CLICK = "org.blitzortung.android.app.ACTION_WIDGET_CLICK"
        private const val DOUBLE_CLICK_THRESHOLD_MS = 500L

        @Volatile
        private var lastClickTime = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(Main.LOG_TAG, "WidgetClickReceiver.onReceive() action=${intent.action}")
        if (intent.action != ACTION_WIDGET_CLICK) {
            return
        }

        val currentTime = System.currentTimeMillis()
        val previousClickTime = lastClickTime
        lastClickTime = currentTime

        if (currentTime - previousClickTime < DOUBLE_CLICK_THRESHOLD_MS) {
            // Double click detected - open the main app
            Log.v(Main.LOG_TAG, "WidgetClickReceiver: double-click detected, opening main app")
            val mainIntent = Intent(context, Main::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mainIntent)
        } else {
            // Single click - trigger widget update
            Log.v(Main.LOG_TAG, "WidgetClickReceiver: single click detected, triggering update")
            showProgressIndicator(context)
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    private fun showProgressIndicator(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, WidgetProvider::class.java)
        )
        if (appWidgetIds.isEmpty()) return

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget)
            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }
    }
}
