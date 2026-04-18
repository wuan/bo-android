package org.blitzortung.android.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.blitzortung.android.alert.handler.Strikes
import org.blitzortung.android.app.view.AlarmView
import org.blitzortung.android.data.DataArea
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.provider.calculateLocalCoordinate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import org.blitzortung.android.alert.LocalActivity

open class WidgetUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private var df: DateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    protected open fun getAppWidgetManager(): AppWidgetManager = AppWidgetManager.getInstance(applicationContext)

    override fun doWork(): Result {
        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() started")

        val appWidgetManager = getAppWidgetManager()
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(applicationContext, WidgetProvider::class.java)
        )

        if (appWidgetIds.isEmpty()) {
            Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() no widgets found")
            return Result.success()
        }

        val app = applicationContext as? BOApplication
        if (app == null) {
            Log.e(Main.LOG_TAG, "WidgetUpdateWorker.doWork() failed: BOApplication not available")
            return Result.failure()
        }

        val component = app.component

        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() got component, starting update")

        val colorHandler = component.strikeColorHandler()
        val alertHandler = component.alertHandler()
        val alertDataHandler = component.alertDataHandler()
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val dataProvider = component.jsonRpcDataProvider()

        var anyWidgetUpdated = false

        try {
            // Get last known location directly without starting the provider (avoids thread issue)
            val location = getLastKnownLocation(locationManager)
            Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() got location: $location")

            for (appWidgetId in appWidgetIds) {
                try {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

                    val displayMetrics = applicationContext.resources.displayMetrics
                    val density = displayMetrics.density
                    val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                    val widthPx = ((if (minWidthDp > 0) minWidthDp else 100) * density).toInt()
                    val heightPx = ((if (minHeightDp > 0) minHeightDp else 100) * density).toInt()

                    val alarmView = AlarmView(applicationContext)
                    alarmView.setColorHandler(colorHandler, 60)

                    var statusText: String? = null

                    if (location != null) {
                        val scale = 5
                        val x = calculateLocalCoordinate(location.longitude, scale)
                        val y = calculateLocalCoordinate(location.latitude, scale)
                        val dataArea = DataArea(x, y, scale)

                        val parameters = Parameters(
                            region = 0,
                            gridSize = 5000,
                            interval = TimeInterval(duration = 60),
                            dataArea = dataArea
                        )

                        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() fetching strike data")
                        val result = dataProvider.retrieveData {
                            getStrikesGrid(parameters, null, Flags())
                        }
                        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() fetched strike data")

                        val strikes = result.strikes?.let { Strikes(it, result.gridParameters) }

                        if (strikes != null) {
                            val alertResult = alertDataHandler.checkStrikes(
                                strikes, location, alertHandler.alertParameters, result.referenceTime
                            )

                            if (alertResult is LocalActivity) {
                                statusText = alertResult.toString()
                            }

                            alarmView.alertEventConsumer.invoke(alertResult)
                        } else {
                            statusText = applicationContext.getString(R.string.widget_no_strike_data)
                        }
                    } else {
                        statusText = applicationContext.getString(R.string.widget_location_not_available)
                    }

                    // AlarmView forces square — measure with min(width, height)
                    val diagramSize = min(widthPx, heightPx)
                    val spec = View.MeasureSpec.makeMeasureSpec(diagramSize, View.MeasureSpec.EXACTLY)
                    alarmView.measure(spec, spec)
                    alarmView.layout(0, 0, alarmView.measuredWidth, alarmView.measuredHeight)

                    val bitmap = Bitmap.createBitmap(
                        alarmView.measuredWidth * 2,
                        alarmView.measuredHeight * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    canvas.scale(2f, 2f)
                    alarmView.draw(canvas)

                    val displayText = if (statusText != null) {
                        "%s @ %s".format(statusText, df.format(Date()))
                    } else {
                        df.format(Date())
                    }
                    val views = RemoteViews(applicationContext.packageName, R.layout.widget)

                    val intent = Intent(applicationContext, WidgetClickReceiver::class.java).apply {
                        action = WidgetClickReceiver.ACTION_WIDGET_CLICK
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        applicationContext, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.alarm_widget, pendingIntent)
                    views.setImageViewBitmap(R.id.alarm_diagram, bitmap)
                    views.setTextViewText(R.id.widget_update_time, displayText)
                    views.setViewVisibility(R.id.widget_progress, View.GONE)
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                    anyWidgetUpdated = true
                } catch (e: Throwable) {
                    Log.e(Main.LOG_TAG, "WidgetUpdateWorker.doWork() failed for widget $appWidgetId", e)
                }
            }
        } catch (e: Exception) {
            Log.e(Main.LOG_TAG, "WidgetUpdateWorker.doWork() failed", e)
            return if (anyWidgetUpdated) {
                Result.success()
            } else {
                // Retry for transient failures like network errors
                Result.retry()
            }
        }

        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() completed successfully")
        return Result.success()
    }

    protected fun getLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }
            } catch (e: SecurityException) {
                Log.w(Main.LOG_TAG, "No permission for location provider: $provider")
            } catch (e: Exception) {
                Log.w(Main.LOG_TAG, "Failed to get location from provider: $provider", e)
            }
        }

        return bestLocation
    }
}
