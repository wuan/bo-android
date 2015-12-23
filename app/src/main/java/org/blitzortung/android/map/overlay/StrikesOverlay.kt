package org.blitzortung.android.map.overlay

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import android.text.format.DateFormat
import android.util.Log
import com.google.android.maps.ItemizedOverlay
import com.google.android.maps.MapView
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.RasterParameters
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.map.OwnMapActivity
import org.blitzortung.android.map.components.LayerOverlayComponent
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.blitzortung.android.map.overlay.color.StrikeColorHandler

class StrikesOverlay(mapActivity: OwnMapActivity, private val colorHandler: StrikeColorHandler) : PopupOverlay<StrikeOverlayItem>(mapActivity, ItemizedOverlay.boundCenter(StrikesOverlay.DEFAULT_DRAWABLE)), LayerOverlay {

    // VisibleForTesting
    protected var strikes: List<StrikeOverlayItem>
    private val layerOverlayComponent: LayerOverlayComponent
    private var zoomLevel: Int = 0
    var rasterParameters: RasterParameters? = null
    var referenceTime: Long = 0
    var parameters = Parameters()

    init {
        layerOverlayComponent = LayerOverlayComponent(mapActivity.resources.getString(R.string.strikes_layer))
        strikes = listOf()
        populate()
    }

    override fun createItem(index: Int): StrikeOverlayItem {
        return strikes[index]
    }

    override fun size(): Int {
        return strikes.size
    }

    override fun draw(canvas: Canvas?, mapView: com.google.android.maps.MapView?, shadow: Boolean) {
        if (!shadow) {
            super.draw(canvas, mapView, false)

            if (hasRasterParameters() && canvas != null && mapView != null) {
                drawDataAreaRect(canvas, mapView)
            }
        }
    }

    private fun drawDataAreaRect(canvas: Canvas, mapView: MapView) {
        val paint = Paint()
        paint.color = colorHandler.lineColor
        paint.style = Style.STROKE

        val clipBounds = canvas.clipBounds
        val currentRasterParameters = rasterParameters
        if (currentRasterParameters != null) {
            val rect = currentRasterParameters.getRect(mapView.projection)

            if (rect.left >= clipBounds.left && rect.left <= clipBounds.right) {
                canvas.drawLine(rect.left, Math.max(rect.top, clipBounds.top.toFloat()), rect.left, Math.min(rect.bottom, clipBounds.bottom.toFloat()), paint)
            }
            if (rect.right >= clipBounds.left && rect.right <= clipBounds.right) {
                canvas.drawLine(rect.right, Math.max(rect.top, clipBounds.top.toFloat()), rect.right, Math.min(rect.bottom, clipBounds.bottom.toFloat()), paint)
            }
            if (rect.bottom <= clipBounds.bottom && rect.bottom >= clipBounds.top) {
                canvas.drawLine(Math.max(rect.left, clipBounds.left.toFloat()), rect.bottom, Math.min(rect.right, clipBounds.right.toFloat()), rect.bottom, paint)
            }
            if (rect.top <= clipBounds.bottom && rect.top >= clipBounds.top) {
                canvas.drawLine(Math.max(rect.left, clipBounds.left.toFloat()), rect.top, Math.min(rect.right, clipBounds.right.toFloat()), rect.top, paint)
            }
        }
    }

    fun addStrikes(strikes: List<Strike>) {
        Log.v(Main.LOG_TAG, "StrikesOverlay.addStrikes() #" + strikes.size)
        this.strikes = strikes.map { StrikeOverlayItem(it) }
        lastFocusedIndex = -1
        populate()
    }

    fun expireStrikes() {
        val expireTime = referenceTime - (parameters.intervalDuration - parameters.intervalOffset) * 60 * 1000

        strikes = strikes.filter { it.timestamp > expireTime }
    }

    fun clear() {
        lastFocusedIndex = -1
        clearPopup()
        strikes = listOf()
        populate()
    }

    fun updateZoomLevel(zoomLevel: Int) {
        if (hasRasterParameters() || zoomLevel != this.zoomLevel) {
            this.zoomLevel = zoomLevel
            refresh()
        }
    }

    fun getColorHandler(): ColorHandler {
        return colorHandler
    }

    fun refresh() {

        val current_section = -1

        colorHandler.updateTarget()

        var drawable: Shape? = null

        for (item in strikes) {
            val section = colorHandler.getColorSection(
                    if (hasRealtimeData())
                        System.currentTimeMillis()
                    else
                        referenceTime,
                    item.timestamp, parameters)

            if (hasRasterParameters() || current_section != section) {
                drawable = updateAndReturnDrawable(item, section, colorHandler)
            } else {
                if (drawable != null) {
                    item.shape = drawable
                }
            }
        }
    }

    // VisibleForTesting
    protected fun updateAndReturnDrawable(item: StrikeOverlayItem, section: Int, colorHandler: ColorHandler): Shape {
        val projection = activity.mapView.projection
        val color = colorHandler.getColor(section)
        val textColor = colorHandler.textColor

        item.updateShape(rasterParameters, projection, color, textColor, zoomLevel)

        return item.shape!!
    }

    fun hasRasterParameters(): Boolean {
        return rasterParameters != null
    }

    fun hasRealtimeData(): Boolean {
        return parameters.isRealtime()
    }

    override fun onTap(index: Int): Boolean {
        val item = strikes[index]
        if (item.point != null && item.timestamp > 0) {
            var result = DateFormat.format("kk:mm:ss", item.timestamp) as String

            if (item.multiplicity > 1) {
                result += ", #%d".format(item.multiplicity)
            }
            showPopup(item.point, result)
            return true
        }
        return false
    }

    val totalNumberOfStrikes: Int
        get() = strikes.fold(0, { previous, item -> previous + item.multiplicity })

    override val name: String
        get() = layerOverlayComponent.name

    override var enabled: Boolean
        get() = layerOverlayComponent.enabled
        set(value) {
            layerOverlayComponent.enabled = value
        }

    override var visible: Boolean
        get() = layerOverlayComponent.visible
        set(value) {
            layerOverlayComponent.visible = value
        }

    companion object {
        private val DEFAULT_DRAWABLE: Drawable

        init {
            val shape = StrikeShape()
            shape.update(1f, 0)
            DEFAULT_DRAWABLE = ShapeDrawable(shape)
        }
    }
}