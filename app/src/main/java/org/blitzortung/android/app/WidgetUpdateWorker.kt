package org.blitzortung.android.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.content.SharedPreferences
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
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
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
import org.blitzortung.android.alert.AlertLabelHandler
import org.blitzortung.android.app.R.color.Green
import org.blitzortung.android.app.R.color.Yellow
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.blitzortung.android.location.LocationHandler

open class WidgetUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        private const val MAX_BITMAP_SIZE = 1024
    }

    private var df: DateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    protected open fun getAppWidgetManager(): AppWidgetManager = AppWidgetManager.getInstance(applicationContext)

    override fun doWork(): Result {
        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() started")

        val appWidgetManager = getAppWidgetManager()
        val appWidgetIds = getWidgetIds(appWidgetManager)

        if (appWidgetIds.isEmpty()) {
            Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() no widgets found")
            return Result.success()
        }

        val appComponents = getAppComponents()
        if (appComponents == null) {
            Log.e(Main.LOG_TAG, "WidgetUpdateWorker.doWork() failed: BOApplication not available")
            return Result.failure()
        }

        val location = resolveLocation(appComponents.locationManager, appComponents.preferences)

        return updateWidgets(appWidgetIds, appWidgetManager, appComponents, location)
    }

    private fun getWidgetIds(appWidgetManager: AppWidgetManager): IntArray {
        return appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(applicationContext, WidgetProvider::class.java)
        )
    }

    protected data class AppComponents(
        val colorHandler: org.blitzortung.android.map.overlay.color.StrikeColorHandler,
        val alertHandler: org.blitzortung.android.alert.handler.AlertHandler,
        val alertDataHandler: org.blitzortung.android.alert.handler.AlertDataHandler,
        val locationManager: LocationManager,
        val dataProvider: org.blitzortung.android.data.provider.standard.JsonRpcDataProvider,
        val preferences: SharedPreferences
    )

    private fun getAppComponents(): AppComponents? {
        val app = applicationContext as? BOApplication ?: return null
        val component = app.component

        return AppComponents(
            colorHandler = component.strikeColorHandler(),
            alertHandler = component.alertHandler(),
            alertDataHandler = component.alertDataHandler(),
            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager,
            dataProvider = component.jsonRpcDataProvider(),
            preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
    }

    private fun resolveLocation(locationManager: LocationManager, preferences: SharedPreferences): Location? {
        val locationMode = preferences.get(PreferenceKey.LOCATION_MODE, LocationHandler.MANUAL_PROVIDER)
        val manualLocation = getManualLocation(preferences)

        val location = when {
            locationMode == LocationHandler.MANUAL_PROVIDER && manualLocation != null -> {
                Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() using manual location")
                manualLocation
            }
            locationMode != LocationHandler.MANUAL_PROVIDER -> {
                getLastKnownLocationFromProvider(locationManager, locationMode)
            }
            else -> {
                getLastKnownLocation(locationManager)
            }
        }
        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() got location: $location")
        return location
    }

    private fun updateWidgets(
        appWidgetIds: IntArray,
        appWidgetManager: AppWidgetManager,
        appComponents: AppComponents,
        location: Location?
    ): Result {
        var anyWidgetUpdated = false

        try {
            for (appWidgetId in appWidgetIds) {
                try {
                    updateSingleWidget(appWidgetId, appWidgetManager, appComponents, location)
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
                Result.retry()
            }
        }

        Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() completed successfully")
        return Result.success()
    }

    private fun updateSingleWidget(
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager,
        appComponents: AppComponents,
        location: Location?
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val displayMetrics = applicationContext.resources.displayMetrics

        val (widthPx, heightPx) = calculateWidgetDimensions(options, displayMetrics)
        val alarmView = createAlarmView(appComponents.colorHandler)
        val (statusText, statusColor) = fetchStrikeData(appComponents, location, alarmView)
        val bitmap = renderWidgetBitmap(alarmView, widthPx, heightPx)
        val displayText = formatDisplayText(statusText)
        val views = buildRemoteViews(bitmap, displayText, statusColor)

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }

    private fun calculateWidgetDimensions(options: android.os.Bundle, displayMetrics: android.util.DisplayMetrics): Pair<Int, Int> {
        val density = displayMetrics.density
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        val widthPx = ((if (minWidthDp > 0) minWidthDp else 100) * density).toInt()
        val heightPx = ((if (minHeightDp > 0) minHeightDp else 100) * density).toInt()

        return Pair(widthPx, heightPx)
    }

    private fun createAlarmView(colorHandler: org.blitzortung.android.map.overlay.color.StrikeColorHandler): AlarmView {
        val alarmView = AlarmView(applicationContext)
        alarmView.setColorHandler(colorHandler, 60)
        return alarmView
    }

    protected fun fetchStrikeData(
        appComponents: AppComponents,
        location: Location?,
        alarmView: AlarmView
    ): Pair<String?, Int> {
        var statusText: String? = null
        var statusColorResource: Int = Green
        var alertResult: Any? = null

        if (location != null) {
            val scale = 5
            val x = calculateLocalCoordinate(location.longitude, scale)
            val y = calculateLocalCoordinate(location.latitude, scale)
            val dataArea = DataArea(x, y, scale)

            val parameters = Parameters(
                region = LOCAL_REGION,
                gridSize = 5000,
                interval = TimeInterval(duration = 60),
                dataArea = dataArea
            )

            Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() fetching strike data")
            val result = appComponents.dataProvider.retrieveData {
                getStrikesGrid(parameters, null, Flags())
            }
            Log.v(Main.LOG_TAG, "WidgetUpdateWorker.doWork() fetched strike data")

            val strikes = result.strikes?.let { Strikes(it, result.gridParameters) }

            if (strikes != null) {
                alertResult = appComponents.alertDataHandler.checkStrikes(
                    strikes, location, appComponents.alertHandler.alertParameters, result.referenceTime
                )

                if (alertResult is LocalActivity) {
                    AlertLabelHandler.extractStatus(alertResult, this.applicationContext).let { (text, colorResource) ->
                        statusText = text
                        statusColorResource = colorResource
                    }
                }

                alarmView.alertEventConsumer.invoke(alertResult)
            } else {
                statusText = applicationContext.getString(R.string.widget_no_strike_data)
                statusColorResource = Yellow
            }
        } else {
            statusText = applicationContext.getString(R.string.widget_location_not_available)
            statusColorResource = Yellow
        }

        return Pair(statusText, applicationContext.getColor(statusColorResource))
    }

    private fun renderWidgetBitmap(alarmView: AlarmView, widthPx: Int, heightPx: Int): Bitmap {
        val diagramSize = min(widthPx, heightPx)
        val spec = View.MeasureSpec.makeMeasureSpec(diagramSize, View.MeasureSpec.EXACTLY)
        alarmView.measure(spec, spec)
        alarmView.layout(0, 0, alarmView.measuredWidth, alarmView.measuredHeight)

        var targetWidth = alarmView.measuredWidth * 2
        var targetHeight = alarmView.measuredHeight * 2

        if (targetWidth > MAX_BITMAP_SIZE || targetHeight > MAX_BITMAP_SIZE) {
            val scale = MAX_BITMAP_SIZE.toFloat() / maxOf(targetWidth, targetHeight)
            targetWidth = (targetWidth * scale).toInt()
            targetHeight = (targetHeight * scale).toInt()
        }

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val canvasScale = targetWidth.toFloat() / alarmView.measuredWidth
        canvas.scale(canvasScale, canvasScale)
        alarmView.draw(canvas)

        return bitmap
    }

    private fun formatDisplayText(statusText: String?): String {
        val updateTime = df.format(Date())
        return if (statusText != null) {
            "%s %s".format(statusText, updateTime)
        } else {
            updateTime
        }
    }

    private fun buildRemoteViews(bitmap: Bitmap, displayText: String, statusColor: Int): RemoteViews {
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
        views.setTextViewText(R.id.widget_status, displayText)
        views.setTextColor(R.id.widget_status, statusColor)
        views.setViewVisibility(R.id.widget_progress, View.GONE)

        return views
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

    protected fun getLastKnownLocationFromProvider(locationManager: LocationManager, provider: String): Location? {
        return try {
            locationManager.getLastKnownLocation(provider)
        } catch (e: SecurityException) {
            Log.w(Main.LOG_TAG, "No permission for location provider: $provider")
            null
        } catch (e: Exception) {
            Log.w(Main.LOG_TAG, "Failed to get location from provider: $provider", e)
            null
        }
    }

    protected fun getManualLocation(preferences: SharedPreferences): Location? {
        val longitudeStr = preferences.getString(PreferenceKey.LOCATION_LONGITUDE.key, null)
        val latitudeStr = preferences.getString(PreferenceKey.LOCATION_LATITUDE.key, null)

        val longitude = longitudeStr?.toDoubleOrNull()
        val latitude = latitudeStr?.toDoubleOrNull()

        return if (longitude != null && latitude != null) {
            Location(LocationManager.GPS_PROVIDER).also {
                it.longitude = longitude
                it.latitude = latitude
                it.accuracy = 0f // Manual location is exact
            }
        } else {
            null
        }
    }
}
