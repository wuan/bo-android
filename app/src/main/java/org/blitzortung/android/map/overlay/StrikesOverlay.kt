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

    var strikes: List<StrikeOverlayItem>
        protected set

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
        this.strikes += strikes.map { StrikeOverlayItem(it) }
        lastFocusedIndex = -1
        try {
            populate()
        } catch (throwable: Throwable) {
            Log.w(Main.LOG_TAG, throwable)
        }
    }

    fun expireStrikes() {
        val expireTime = referenceTime - parameters.intervalDuration * 60 * 1000

        val sizeBefore = strikes.size
        strikes = strikes.filter { it.timestamp > expireTime }
        Log.v(Main.LOG_TAG, "StrikesOverlay.expireStrikes() expired ${sizeBefore - strikes.size} from $sizeBefore")
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
                    item.timestamp, parameters.intervalDuration)

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

            if (item.shape is RasterShape) {
                result += ", #%d".format(item.multiplicity)
            } else if (item.shape is StrikeShape) {
                result += " (%.4f %.4f)".format(item.longitude, item.latitude)
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