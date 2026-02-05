package org.blitzortung.android.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.blitzortung.android.alert.handler.AlertDataHandler
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.alert.handler.Strikes
import org.blitzortung.android.app.view.AlarmView
import org.blitzortung.android.data.DataArea
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.provider.calculateLocalCoordinate
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal class WidgetUpdateTask(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val appWidgetIds: IntArray,
    private val colorHandler: StrikeColorHandler,
    private val alertHandler: AlertHandler,
    private val alertDataHandler: AlertDataHandler,
    private val locationHandler: LocationHandler,
    private val dataProvider: DataProvider
) : AsyncTask<Unit, Unit, Unit>() {

    private var df: DateFormat = SimpleDateFormat("hh:mm:ss")

    override fun doInBackground(vararg params: Unit) {
        try {
            locationHandler.start()

            appWidgetIds.forEach { appWidgetId ->
                val views = getUpdatedViews(appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Throwable) {
            Log.e(Main.LOG_TAG, "WidgetUpdateTask.doInBackground() failed", e)
        } finally {
            locationHandler.shutdown()
        }
    }

    private fun getUpdatedViews(appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget)
        try {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)

            val displayMetrics = context.resources.displayMetrics
            val density = displayMetrics.density
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

            val width = (if (minWidthDp > 0) minWidthDp else 100) * density
            val height = (if (minHeightDp > 0) minHeightDp else 100) * density

            val alarmView = AlarmView(context)
            alarmView.setColorHandler(colorHandler, 60)

            val location = locationHandler.location

            if (location != null) {
                val scale = 5
                val x = calculateLocalCoordinate(location.longitude, scale)
                val y = calculateLocalCoordinate(location.latitude, scale)
                val dataArea = DataArea(x, y, scale)

                val parameters =
                    Parameters(region = 0, gridSize = 5000, interval = TimeInterval(duration = 60), dataArea = dataArea)

                val result: DataReceived = dataProvider.retrieveData {
                    getStrikesGrid(parameters, null, Flags())
                }

                val strikes = result.strikes?.let { Strikes(it, result.gridParameters) }

                if (strikes != null) {
                    val alertResult =
                        alertDataHandler.checkStrikes(strikes, location, alertHandler.alertParameters, result.referenceTime)

                    alarmView.alertEventConsumer.invoke(alertResult)

                    val widthSpec = View.MeasureSpec.makeMeasureSpec(width.toInt(), View.MeasureSpec.EXACTLY)
                    val heightSpec = View.MeasureSpec.makeMeasureSpec(height.toInt(), View.MeasureSpec.EXACTLY)
                    alarmView.measure(widthSpec, heightSpec)
                    alarmView.layout(0, 0, alarmView.measuredWidth, alarmView.measuredHeight)

                    val bitmap = Bitmap.createBitmap(alarmView.measuredWidth, alarmView.measuredHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    alarmView.draw(canvas)

                    views.setImageViewBitmap(R.id.alarm_diagram, bitmap)
                } else {
                    views.setTextViewText(R.id.widget_update_time, "no strike data")
                }
            } else {
                views.setTextViewText(R.id.widget_update_time, "location not available")
            }

        } catch (e: Throwable) {
            Log.e(Main.LOG_TAG, "WidgetUpdateTask.getUpdatedViews() failed", e)
            views.setTextViewText(R.id.widget_update_time, "update failed")
        }

        val format = df.format(Date())
        views.setTextViewText(R.id.widget_update_time, format)

        return views
    }
}
