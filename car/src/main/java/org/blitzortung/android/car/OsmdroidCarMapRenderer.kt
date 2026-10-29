/*

   Copyright 2026 Andreas Würl

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

package org.blitzortung.android.car

import android.app.Presentation
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import androidx.preference.PreferenceManager
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/**
 * Renderer that reuses the phone map's osmdroid overlays by hosting a [MapView]
 * in a [Presentation] on a [VirtualDisplay] backed by the host's surface.
 */
class OsmdroidCarMapRenderer(
    private val carContext: CarContext,
    strikeColorHandler: StrikeColorHandler,
) : CarMapRenderer {
    init {
        Configuration.getInstance().apply {
            load(carContext, PreferenceManager.getDefaultSharedPreferences(carContext))
            userAgentValue = carContext.packageName
        }
    }

    internal val mapView: MapView =
        MapView(carContext).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            setUseDataConnection(true)
            setBuiltInZoomControls(false)
        }

    internal val strikeListOverlay: StrikeListOverlay = StrikeListOverlay(mapView, strikeColorHandler)

    internal val ownLocationOverlay: OwnLocationOverlay = OwnLocationOverlay(carContext, mapView)

    init {
        mapView.overlays.add(strikeListOverlay)
        mapView.overlays.add(ownLocationOverlay)
        mapView.addMapListener(strikeListOverlay)
        mapView.addMapListener(ownLocationOverlay)
    }

    private val displayManager: DisplayManager =
        carContext.getSystemService(DisplayManager::class.java)

    private var virtualDisplay: VirtualDisplay? = null

    private var presentation: Presentation? = null

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val display =
            displayManager.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                surfaceContainer.width,
                surfaceContainer.height,
                surfaceContainer.dpi,
                surfaceContainer.surface,
                0,
            ) ?: return
        virtualDisplay = display

        val activeDisplay = display.display ?: return
        presentation =
            Presentation(carContext, activeDisplay).apply {
                setContentView(mapView)
                show()
            }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        presentation?.dismiss()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }

    override fun onScroll(
        distanceX: Float,
        distanceY: Float,
    ) {
        mapView.controller.scrollBy(-distanceX.toInt(), -distanceY.toInt())
    }

    override fun onFling(
        velocityX: Float,
        velocityY: Float,
    ) {
        // osmdroid has no public fling API; approximate it with a proportional scroll.
        mapView.controller.scrollBy((velocityX / FLING_DIVISOR).toInt(), (velocityY / FLING_DIVISOR).toInt())
    }

    override fun onScale(
        focusX: Float,
        focusY: Float,
        scaleFactor: Float,
    ) {
        if (scaleFactor >= 1f) {
            mapView.controller.zoomInFixing(focusX.toInt(), focusY.toInt())
        } else {
            mapView.controller.zoomOutFixing(focusX.toInt(), focusY.toInt())
        }
    }

    override fun onClick(
        x: Float,
        y: Float,
    ) {
        // Strike details are a phone-only affordance; the car map has no popup.
    }

    override fun setDarkMode(darkMode: Boolean) {
        val colorFilter = if (darkMode) TilesOverlay.INVERT_COLORS else null
        mapView.overlayManager.tilesOverlay.setColorFilter(colorFilter)
        mapView.invalidate()
    }

    override fun zoomIn() {
        mapView.controller.zoomIn()
    }

    override fun zoomOut() {
        mapView.controller.zoomOut()
    }

    override fun updateStrikes(event: DataReceived) {
        val parameters = event.parameters
        val initializeOverlay = strikeListOverlay.parameters != parameters

        strikeListOverlay.parameters = parameters
        strikeListOverlay.gridParameters = event.gridParameters
        strikeListOverlay.referenceTime = event.referenceTime

        if (event.updated >= 0 && !initializeOverlay) {
            strikeListOverlay.expireStrikes()
        } else {
            strikeListOverlay.clear()
        }

        event.strikes?.let { strikes ->
            val strikesToAdd =
                if (event.updated > 0 && !initializeOverlay) {
                    strikes.subList(strikes.size - event.updated, strikes.size)
                } else {
                    strikes
                }
            strikeListOverlay.addStrikes(strikesToAdd)
        }

        strikeListOverlay.refresh()
        mapView.invalidate()
    }

    override fun clearStrikes() {
        strikeListOverlay.clear()
        mapView.invalidate()
    }

    override fun onLocationEvent(event: LocationEvent) {
        ownLocationOverlay.locationEventConsumer(event)
    }

    override fun screenPositionOf(
        latitude: Double,
        longitude: Double,
    ): Point {
        val point = Point()
        mapView.projection.toPixels(GeoPoint(latitude, longitude), point)
        return point
    }

    private companion object {
        private const val VIRTUAL_DISPLAY_NAME = "BlitzortungCarMap"
        private const val FLING_DIVISOR = 10f
    }
}
