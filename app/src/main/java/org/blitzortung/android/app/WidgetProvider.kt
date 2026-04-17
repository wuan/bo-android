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
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

open class WidgetProvider : AppWidgetProvider() {

    companion object {
        const val WIDGET_UPDATE_WORK_NAME = "widget_update_work"
        const val UPDATE_INTERVAL_MINUTES = 15L
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.v(Main.LOG_TAG, "WidgetProvider.onUpdate() - re-rendering with new size")
        scheduleImmediateUpdate(context)
        scheduleNextUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.v(Main.LOG_TAG, "WidgetProvider.onEnabled() - Scheduling immediate and periodic updates")
        scheduleImmediateUpdate(context)
        scheduleNextUpdate(context)
    }

    protected open fun getWorkManager(context: Context): WorkManager = WorkManager.getInstance(context)

    private fun scheduleImmediateUpdate(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .build()
        getWorkManager(context).enqueue(workRequest)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.v(Main.LOG_TAG, "WidgetProvider.onDisabled() - Cancelling periodic updates")
        getWorkManager(context).cancelUniqueWork(WIDGET_UPDATE_WORK_NAME)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Log.v(Main.LOG_TAG, "WidgetProvider.onAppWidgetOptionsChanged() - re-rendering with new size")
        scheduleImmediateUpdate(context)
    }

    private fun scheduleNextUpdate(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .build()

        getWorkManager(context).enqueueUniquePeriodicWork(
            WIDGET_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
