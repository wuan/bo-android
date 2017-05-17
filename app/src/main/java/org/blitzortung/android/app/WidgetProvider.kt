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
import android.widget.RemoteViews
import org.blitzortung.android.app.view.AlertView

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val N = appWidgetIds.size
        for (i in 0..N - 1) {
            val appWidgetId = appWidgetIds[i]
            updateAppWidget(context)
        }
    }

    private fun updateAppWidget(context: Context) {

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
