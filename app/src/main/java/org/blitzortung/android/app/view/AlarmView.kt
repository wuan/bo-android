/*

   Copyright 2025 Andreas Würl

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

package org.blitzortung.android.app.view

import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.util.AttributeSet
import android.util.Log
import androidx.preference.PreferenceManager
import kotlin.math.max
import kotlin.math.min
import org.blitzortung.android.alert.LocalActivity
import org.blitzortung.android.alert.NoLocation
import org.blitzortung.android.alert.Outlying
import org.blitzortung.android.alert.Warning
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.helper.ViewHelper
import org.blitzortung.android.app.view.alarm.LocalActivityRenderer
import org.blitzortung.android.app.view.alarm.PrimitiveRenderer
import org.blitzortung.android.app.view.alarm.SymbolRenderer
import org.blitzortung.android.app.view.support.CanvasProvider
import org.blitzortung.android.app.view.support.CanvasWrapper
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.dialogs.AlarmDialog
import org.blitzortung.android.dialogs.AlertDialogColorHandler
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.location.LocationUpdate
import org.blitzortung.android.map.overlay.color.ColorHandler
import org.blitzortung.android.util.TabletAwareView


data class AlarmViewData(
    var size: Int = 0,
    var center: Float = 0f,
    var radius: Float = 0f,
)

class AlarmView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    canvasProvider: CanvasProvider? = null,
    primitiveRenderer: PrimitiveRenderer? = null,
    symbolRenderer: SymbolRenderer? = null,
    localActivityRenderer: LocalActivityRenderer? = null
) : TabletAwareView(context, attrs, defStyle) {

    private lateinit var colorHandler: ColorHandler
    private var intervalDuration: Int = 0
    private var warning: Warning? = null
    private var location: Location? = null
    private var enableDescriptionText = false

    private val canvasProvider = canvasProvider ?: CanvasProvider(width, height)
    private val primitiveRenderer: PrimitiveRenderer = primitiveRenderer ?: PrimitiveRenderer()
    private var symbolRenderer: SymbolRenderer =
        symbolRenderer ?: SymbolRenderer(context, this.primitiveRenderer, textSize)
    private val localActivityRenderer: LocalActivityRenderer = localActivityRenderer ?: LocalActivityRenderer(
        context,
        this.primitiveRenderer, textSize * textSizeFactor(context)
    )

    private var drawCanvas: CanvasWrapper? = null

    private val alarmViewData = AlarmViewData()

    val alertEventConsumer: (Warning) -> Unit = { event ->
        val updated = warning != event
        if (updated) {
            Log.v(Main.LOG_TAG, "AlertView alertEventConsumer received $event")
            warning = event
            invalidate()
        }
    }

    val locationEventConsumer: (LocationEvent) -> Unit = { locationEvent ->
        val newLocation = if (locationEvent is LocationUpdate) locationEvent.location else null
        if (location != newLocation) {
            Log.v(Main.LOG_TAG, "AlertView received location ${newLocation}")
            location = newLocation
            val visibility = if (location != null) VISIBLE else INVISIBLE
            setVisibility(visibility)
            invalidate()
        }
    }

    fun setColorHandler(
        colorHandler: ColorHandler,
        intervalDuration: Int,
    ) {
        this.colorHandler = colorHandler
        this.intervalDuration = intervalDuration

        symbolRenderer.colorHandler = colorHandler
        localActivityRenderer.colorHandler = colorHandler
        localActivityRenderer.intervalDuration = intervalDuration
    }

    fun enableLongClickListener(
        dataHandler: MainDataHandler,
        alertHandler: AlertHandler,
    ) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        setOnLongClickListener {
            AlarmDialog(context, AlertDialogColorHandler(sharedPreferences), dataHandler, alertHandler)
                .show()
            true
        }
    }

    fun enableDescriptionText() {
        enableDescriptionText = true
        localActivityRenderer.enableDescriptionText = true
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val getSize = fun(spec: Int) = MeasureSpec.getSize(spec)

        val parentWidth = getSize(widthMeasureSpec) * sizeFactor
        val parentHeight = getSize(heightMeasureSpec) * sizeFactor

        val size = min(parentWidth.toInt(), parentHeight.toInt())

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val size = max(width, height)
        val pad = ViewHelper.pxFromDp(context, 5f)

        val center = size / 2.0f
        val radius = center - pad

        with(alarmViewData) {
            this.size = size
            this.center = center
            this.radius = radius
        }

        drawCanvas = canvasProvider.provide(colorHandler.backgroundColor, width, height)

        drawCanvas?.also { drawCanvas ->
            drawCanvas.clear()
            val warning = this@AlarmView.warning
            when (warning) {
                is LocalActivity if intervalDuration != 0 -> {
                    localActivityRenderer.renderLocalActivity(warning, alarmViewData, drawCanvas)
                }

                Outlying -> {
                    symbolRenderer.drawOutOfRangeSymbol(alarmViewData, drawCanvas.canvas)
                }

                NoLocation -> {
                    if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
                        symbolRenderer.drawAlertOrLocationMissingMessage(center, width, drawCanvas.canvas)
                    } else {
                        symbolRenderer.drawNoLocationSymbol(alarmViewData, drawCanvas.canvas)
                    }
                }

                else -> {
                    if (enableDescriptionText && size > TEXT_MINIMUM_SIZE) {
                        symbolRenderer.drawAlertOrLocationMissingMessage(center, width, drawCanvas.canvas)
                    } else {
                        if (location != null) {
                            symbolRenderer.drawOwnLocationSymbol(alarmViewData, drawCanvas.canvas)
                        } else {
                            symbolRenderer.drawNoLocationSymbol(alarmViewData, drawCanvas.canvas)
                        }
                    }
                }
            }
            drawCanvas.update(canvas)
        }
    }


    companion object {
        private const val TEXT_MINIMUM_SIZE = 300
    }
}
