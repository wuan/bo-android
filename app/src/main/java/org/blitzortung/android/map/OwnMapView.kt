package org.blitzortung.android.map

import android.content.Context
import android.graphics.Canvas
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

import com.google.android.maps.MapView
import org.blitzortung.android.app.Main

import org.blitzortung.android.app.R
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.location.LocationHandler

import java.util.HashSet

class OwnMapView : MapView {

    private val zoomListeners = HashSet<(Int) -> Unit>()

    private val gestureDetector: GestureDetector = GestureDetector(context, GestureListener())

    private var oldPixelSize = -1f

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    constructor(context: Context, apiKey: String) : super(context, apiKey) {
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {

            this@OwnMapView.removeView(popup)

            controller.animateTo(getPoint(event))
            controller.zoomIn()
            return true
        }

        private fun getPoint(event: MotionEvent) = projection.fromPixels(event.x.toInt(), event.y.toInt())

        override fun onLongPress(event: MotionEvent) {
            val point = getPoint(event)
            val longitude = point.longitudeE6 / 1e6
            val latitude = point.latitudeE6 / 1e6
            Log.v(Main.LOG_TAG, "GestureListener.onLongPress() $point")
            val context = this@OwnMapView.context
            val locationText = context.resources.getString(R.string.set_manual_location)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            editor.putString(PreferenceKey.LOCATION_LONGITUDE.toString(), longitude.toString())
            editor.putString(PreferenceKey.LOCATION_LATITUDE.toString(), latitude.toString())
            editor.putString(PreferenceKey.LOCATION_MODE.toString(), LocationHandler.Provider.MANUAL.type)
            editor.apply()
            Toast.makeText(context, "%s: %.4f %.4f".format(locationText, longitude, latitude),
                    Toast.LENGTH_LONG).show()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        detectAndHandleZoomAction()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val result = super.onTouchEvent(event)

        gestureDetector!!.onTouchEvent(event)

        return result
    }

    protected fun detectAndHandleZoomAction() {
        if (projection != null) {
            val pixelSize = projection.metersToEquatorPixels(1000.0f)

            if (pixelSize != oldPixelSize) {
                notifyZoomListeners()
                oldPixelSize = pixelSize
            }
        }
    }

    fun addZoomListener(zoomListener: (Int) -> Unit) {
        zoomListeners.add(zoomListener)
    }

    fun notifyZoomListeners() {
        for (zoomListener in zoomListeners) {
            zoomListener.invoke(zoomLevel)
        }
    }

    fun calculateTargetZoomLevel(widthInMeters: Float): Int {
        val equatorLength = 40075004.0 // in meters
        val widthInPixels = Math.min(height, width).toDouble()
        var metersPerPixel = equatorLength / 256
        var zoomLevel = 1
        while ((metersPerPixel * widthInPixels) > widthInMeters) {
            metersPerPixel /= 2.0
            ++zoomLevel
        }
        return zoomLevel - 1
    }

    val popup: View by lazy { LayoutInflater.from(context).inflate(R.layout.popup, this, false) }

}
