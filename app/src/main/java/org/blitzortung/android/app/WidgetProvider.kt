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

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.util.isAtLeast

open class WidgetProvider : AppWidgetProvider() {

    companion object {
        const val WIDGET_UPDATE_WORK_NAME = "widget_update_work"
        const val WIDGET_IMMEDIATE_UPDATE_WORK_NAME = "widget_immediate_update_work"
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
        showDisclosureIfNeeded(context)
    }

    private fun showDisclosureIfNeeded(context: Context) {
        if (!isAtLeast(Build.VERSION_CODES.Q)) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val disclosed = prefs.get(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, false)
        val hasPermission = context.checkSelfPermission(ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!disclosed && !hasPermission) {
            val intent = Intent(context, BackgroundLocationDisclosureActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    protected open fun getWorkManager(context: Context): WorkManager = WorkManager.getInstance(context)

    private fun scheduleImmediateUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setConstraints(constraints)
            .build()
        getWorkManager(context).enqueueUniqueWork(
            WIDGET_IMMEDIATE_UPDATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
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
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        getWorkManager(context).enqueueUniquePeriodicWork(
            WIDGET_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
