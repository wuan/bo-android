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

package org.blitzortung.android.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.widget.RemoteViews
import org.blitzortung.android.app.view.AlertView
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.jetbrains.anko.defaultSharedPreferences

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (i in 0 until appWidgetIds.size) {
            val appWidgetId = appWidgetIds[i]
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        Log.v(Main.LOG_TAG, "updateAppWidget $appWidgetId")
        val alertView = AlertView(context)
        val size = 150
        alertView.measure(size, size)
        alertView.layout(0, 0, size, size)
        val preferences = context.defaultSharedPreferences
        val colorHandler = StrikeColorHandler(preferences)
        alertView.setColorHandler(colorHandler, 10)

        val temporaryBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val temporaryCanvas = Canvas(temporaryBitmap)
        val background = Paint()
        background.color = colorHandler.backgroundColor
        background.xfermode = AlertView.XFERMODE_CLEAR
        temporaryCanvas.drawPaint(background)
        val foreground = Paint()
        foreground.color = colorHandler.lineColor
        temporaryCanvas.drawLine(0f, 0f, size.toFloat(), size.toFloat(), foreground);

        background.xfermode = AlertView.XFERMODE_SRC
        alertView.draw(temporaryCanvas)
        val remoteViews = RemoteViews(context.packageName, R.layout.widget)
        remoteViews.setImageViewBitmap(R.layout.widget, temporaryBitmap)

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)

    }
}
