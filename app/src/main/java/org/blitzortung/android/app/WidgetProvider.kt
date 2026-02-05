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
import androidx.work.*
import dagger.android.AndroidInjection
import org.blitzortung.android.alert.handler.AlertDataHandler
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WidgetProvider : AppWidgetProvider() {

    companion object {
        private const val WIDGET_UPDATE_WORK_NAME = "widget_update_work"
        private const val UPDATE_INTERVAL_MINUTES = 5L
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        Log.v(Main.LOG_TAG, "WidgetProvider.onAppWidgetOptionsChanged()")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        update(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.v(Main.LOG_TAG, "WidgetProvider.onReceive() $action")
        if (ACTION_APPWIDGET_UPDATE == action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WidgetProvider::class.java)
            )
            update(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.v(Main.LOG_TAG, "WidgetProvider.onUpdate()")
        update(context, appWidgetManager, appWidgetIds)
    }

    private fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val workManager = WorkManager.getInstance(context)

        val inputData = workDataOf("appWidgetIds" to appWidgetIds)

        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInputData(inputData)
            .build()

        workManager.enqueue(workRequest)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.v(Main.LOG_TAG, "WidgetProvider.onEnabled() - Scheduling periodic updates")
        schedulePeriodicUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.v(Main.LOG_TAG, "WidgetProvider.onDisabled() - Cancelling periodic updates")
        cancelPeriodicUpdates(context)
    }

    private fun schedulePeriodicUpdates(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WIDGET_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelPeriodicUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK_NAME)
    }
}
