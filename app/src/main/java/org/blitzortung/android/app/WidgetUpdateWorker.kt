package org.blitzortung.android.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import java.util.*
import kotlin.math.min

class WidgetUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private var df: DateFormat = SimpleDateFormat("HH:mm:ss")

    override fun doWork(): Result {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = inputData.getIntArray("appWidgetIds")

        val app = applicationContext as BOApplication
        val component = app.component
        val colorHandler = component.strikeColorHandler()
        val alertHandler = component.alertHandler()
        val alertDataHandler = component.alertDataHandler()
        val locationHandler = component.locationHandler()
        val dataProvider = component.jsonRpcDataProvider()

        try {
            locationHandler.start()

            if (appWidgetIds != null) {
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
                        val location = locationHandler.location

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

                            val result = dataProvider.retrieveData {
                                getStrikesGrid(parameters, null, Flags())
                            }

                            val strikes = result.strikes?.let { Strikes(it, result.gridParameters) }

                            if (strikes != null) {
                                val alertResult = alertDataHandler.checkStrikes(
                                    strikes, location, alertHandler.alertParameters, result.referenceTime
                                )
                                alarmView.alertEventConsumer.invoke(alertResult)
                            } else {
                                statusText = "no strike data"
                            }
                        } else {
                            statusText = "location not available"
                        }

                        // AlarmView forces square — measure with min(width, height)
                        val diagramSize = min(widthPx, heightPx)
                        val spec = View.MeasureSpec.makeMeasureSpec(diagramSize, View.MeasureSpec.EXACTLY)
                        alarmView.measure(spec, spec)
                        alarmView.layout(0, 0, alarmView.measuredWidth, alarmView.measuredHeight)

                        val bitmap = Bitmap.createBitmap(
                            alarmView.measuredWidth, alarmView.measuredHeight, Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        alarmView.draw(canvas)

                        val views = RemoteViews(applicationContext.packageName, R.layout.widget)
                        views.setImageViewBitmap(R.id.alarm_diagram, bitmap)
                        views.setTextViewText(
                            R.id.widget_update_time,
                            statusText ?: df.format(Date())
                        )
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                    } catch (e: Throwable) {
                        Log.e(Main.LOG_TAG, "WidgetUpdateWorker.doWork() failed for widget $appWidgetId", e)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(Main.LOG_TAG, "WidgetUpdateWorker.doWork() failed", e)
        } finally {
            locationHandler.shutdown()
        }

        return Result.success()
    }
}
