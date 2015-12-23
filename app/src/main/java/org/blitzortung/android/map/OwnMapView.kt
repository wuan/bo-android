package org.blitzortung.android.map

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View

import com.google.android.maps.MapView

import org.blitzortung.android.app.R

import java.util.HashSet

class OwnMapView : MapView {

    private val zoomListeners = HashSet<(Int) -> Unit>()

    private val gestureDetector: GestureDetector

    private var oldPixelSize = -1f

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    }

    constructor(context: Context, apiKey: String) : super(context, apiKey) {
    }

    init {

        gestureDetector = GestureDetector(object : GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(event: MotionEvent): Boolean {

                /*removeView(popup)

                val x = event.x.toInt()
                val y = event.y.toInt()
                val p = projection
                controller.animateTo(p.fromPixels(x, y))
                controller.zoomIn()*/
                return true
            }
        })
    }

    public override fun dispatchDraw(canvas: Canvas) {
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

    interface ZoomListener {
        fun onZoom(zoomLevel: Int)
    }

}
