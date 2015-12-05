package org.blitzortung.android.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews

import org.blitzortung.android.app.view.AlertView

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val N = appWidgetIds.size
        for (i in 0..N - 1) {
            val appWidgetId = appWidgetIds[i]
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int) {

        val alertView = AlertView(context)
        alertView.measure(150, 150)
        alertView.layout(0, 0, 150, 150)
        alertView.isDrawingCacheEnabled = true
        val bitmap = alertView.drawingCache

        val remoteViews = RemoteViews(context.packageName,
                R.layout.widget)
        remoteViews.setImageViewBitmap(R.layout.widget, bitmap)

    }
}
