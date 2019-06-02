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
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import dagger.android.AndroidInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.event.AlertResultEvent
import org.blitzortung.android.alert.handler.AlertDataHandler
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.view.AlertView
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class WidgetProvider : AppWidgetProvider() {

    @set:Inject
    internal lateinit var colorHandler: StrikeColorHandler

    @set:Inject
    internal lateinit var alertHandler: AlertHandler

    @set:Inject
    internal lateinit var alertDataHandler: AlertDataHandler

    @set:Inject
    internal lateinit var locationHandler: LocationHandler

    @set:Inject
    internal lateinit var dataProvider: JsonRpcDataProvider

    var df: DateFormat = SimpleDateFormat("hh:mm:ss")


    override fun onAppWidgetOptionsChanged(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetId: Int, newOptions: Bundle?) {
        AndroidInjection.inject(this, context)
        Log.v(Main.LOG_TAG, "WidgetProvider.onAppWidgetOptionsChanged()")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context?, intent: Intent) {
        AndroidInjection.inject(this, context)
        val action = intent.action
        Log.v(Main.LOG_TAG, "WidgetProvider.onReceive() $action")
        if (ACTION_APPWIDGET_UPDATE == action) {
            locationHandler.start()
            val views = getUpdatedViews(context!!)
            AppWidgetManager.getInstance(context).updateAppWidget(
                    ComponentName(context, WidgetProvider::class.java)
                    , views)
            locationHandler.shutdown()
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AndroidInjection.inject(this, context)
        Log.v(Main.LOG_TAG, "WidgetProvider.onUpdate()")

        locationHandler.start()

        val views = getUpdatedViews(context)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        locationHandler.shutdown()
    }

    private fun getUpdatedViews(context: Context): RemoteViews {
        val alertView = AlertView(context)
        alertView.setColorHandler(colorHandler, 60)

        val parameters = Parameters(region = 0, rasterBaselength = 5000, intervalDuration = 60)
        val result = runBlocking {
            withContext(Dispatchers.Default) {
                async {
                    Log.v(Main.LOG_TAG, "Widget.getUpdatedViews() retrieve running in ${Thread.currentThread().name}")
                    var result = ResultEvent(referenceTime = System.currentTimeMillis(), parameters = parameters, flags = Flags())
                    dataProvider.retrieveData {
                        result = getStrikesGrid(parameters, result)
                    }
                    Log.v(Main.LOG_TAG, "Widget.getUpdatedViews() check running in ${Thread.currentThread().name}")
                    alertDataHandler.checkStrikes(result.strikes!!, locationHandler.location!!, alertHandler.alertParameters, result.referenceTime)
                }.await()
            }
        }

        Log.v(Main.LOG_TAG, "Widget.getUpdatedViews() result in ${Thread.currentThread().name} $result")
        alertView.alertEventConsumer.invoke(AlertResultEvent(result))

        alertView.measure(150, 150)
        alertView.layout(0, 0, 150, 150)
        alertView.isDrawingCacheEnabled = true

        val bitmap = alertView.drawingCache

        val views = RemoteViews(context.packageName, R.layout.widget)
        views.setImageViewBitmap(R.id.alarm_diagram, bitmap)
        val format = df.format(Date())
        Log.v(Main.LOG_TAG, "WidgetProvider.getUpdatedViews() $format")
        views.setTextViewText(R.id.widget_update_time, format)
        return views
    }
}

