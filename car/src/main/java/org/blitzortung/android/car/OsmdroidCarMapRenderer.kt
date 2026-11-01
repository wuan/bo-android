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
import kotlin.math.max
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
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
    private val carSettings: CarSettings,
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

    /**
     * Dimming scrim. Added as the first overlay so it covers the map tiles only,
     * leaving the strike and grid overlays drawn after it undimmed.
     */
    internal val dimOverlay: MapDimOverlay = MapDimOverlay()

    init {
        // Index 0 => drawn directly above the map tiles and below every data
        // overlay (strikes, grid, own location), so only the background dims.
        mapView.overlays.add(0, dimOverlay)
        mapView.overlays.add(strikeListOverlay)
        mapView.overlays.add(ownLocationOverlay)
        mapView.addMapListener(strikeListOverlay)
        mapView.addMapListener(ownLocationOverlay)
    }

    private val displayManager: DisplayManager =
        carContext.getSystemService(DisplayManager::class.java)

    private var virtualDisplay: VirtualDisplay? = null

    private var presentation: Presentation? = null

    /** Whether the camera has been framed on a location fix; framing happens at most once. */
    private var initialLocationFramed = false

    /** Whether the driver has panned or zoomed, which suppresses the initial framing. */
    private var userMovedMap = false

    /** Whether a stored camera was restored, which suppresses the initial framing. */
    private var cameraRestored = false

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        applyInitialCamera(surfaceContainer.width, surfaceContainer.height)

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
        userMovedMap = true
        mapView.controller.scrollBy(-distanceX.toInt(), -distanceY.toInt())
        storeCamera()
    }

    override fun onFling(
        velocityX: Float,
        velocityY: Float,
    ) {
        userMovedMap = true
        // osmdroid has no public fling API; approximate it with a proportional scroll.
        mapView.controller.scrollBy((velocityX / FLING_DIVISOR).toInt(), (velocityY / FLING_DIVISOR).toInt())
        storeCamera()
    }

    override fun onScale(
        focusX: Float,
        focusY: Float,
        scaleFactor: Float,
    ) {
        userMovedMap = true
        if (scaleFactor >= 1f) {
            mapView.controller.zoomInFixing(focusX.toInt(), focusY.toInt())
        } else {
            mapView.controller.zoomOutFixing(focusX.toInt(), focusY.toInt())
        }
        storeCamera()
    }

    override fun onClick(
        x: Float,
        y: Float,
    ) {
        // Strike details are a phone-only affordance; the car map has no popup.
    }

    override fun setDimmed(dimmed: Boolean) {
        dimOverlay.alpha = if (dimmed) DIM_ALPHA else 0
        mapView.invalidate()
    }

    override fun zoomIn() {
        userMovedMap = true
        mapView.controller.zoomIn()
        storeCamera()
    }

    override fun zoomOut() {
        userMovedMap = true
        mapView.controller.zoomOut()
        storeCamera()
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

        val location = event.location() ?: return
        if (initialLocationFramed || userMovedMap || cameraRestored) {
            return
        }
        initialLocationFramed = true

        mapView.controller.setZoom(DEFAULT_ZOOM_LEVEL)
        mapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
        mapView.invalidate()
    }

    private fun storeCamera() {
        val center = mapView.mapCenter
        carSettings.storeCamera(CarCamera(center.latitude, center.longitude, mapView.zoomLevelDouble))
    }

    override fun screenPositionOf(
        latitude: Double,
        longitude: Double,
    ): Point {
        val point = Point()
        mapView.projection.toPixels(GeoPoint(latitude, longitude), point)
        return point
    }

    /**
     * Establishes the camera for the current surface. A stored camera is restored
     * in preference to the world view; without one the map shows the whole world
     * covering the viewport, so it is defined even when no location event arrives.
     */
    internal fun applyInitialCamera(
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) return

        val worldZoom = worldViewZoomFor(width, height)
        mapView.setMinZoomLevel(worldZoom)
        mapView.setScrollableAreaLimitDouble(WORLD_BOUNDS)

        val storedCamera = carSettings.camera
        if (storedCamera != null) {
            cameraRestored = true
            mapView.controller.setZoom(max(storedCamera.zoom, worldZoom))
            mapView.controller.setCenter(GeoPoint(storedCamera.latitude, storedCamera.longitude))
        } else {
            cameraRestored = false
            mapView.controller.setZoom(worldZoom)
            mapView.controller.setCenter(GeoPoint(WORLD_CENTER_LATITUDE, WORLD_CENTER_LONGITUDE))
        }
        mapView.invalidate()
    }

    /** Zoom at which the world covers the viewport in both dimensions. */
    internal fun worldViewZoomFor(
        width: Int,
        height: Int,
    ): Double {
        val tileSystem = MapView.getTileSystem()
        return max(
            tileSystem.getLongitudeZoom(WORLD_LON_EAST, WORLD_LON_WEST, width),
            tileSystem.getLatitudeZoom(WORLD_LAT_NORTH, WORLD_LAT_SOUTH, height),
        )
    }

    internal companion object {
        private const val VIRTUAL_DISPLAY_NAME = "BlitzortungCarMap"
        private const val FLING_DIVISOR = 10f

        /**
         * Opacity of the dimming scrim over the map background. Strong enough to cut
         * night glare while leaving the strike and grid layers readable.
         */
        private const val DIM_ALPHA = 128

        /**
         * Zoom the car map starts at when it first frames the driver's position.
         * Closer than the phone's world view so nearby strikes are legible.
         */
        const val DEFAULT_ZOOM_LEVEL = 8.0

        /** North/south extent of the Web-Mercator world. */
        private const val WORLD_LAT_NORTH = 85.051129
        private const val WORLD_LAT_SOUTH = -85.051129

        /** East/west extent of the world. */
        private const val WORLD_LON_EAST = 180.0
        private const val WORLD_LON_WEST = -180.0

        /** Centre of the world view. */
        private const val WORLD_CENTER_LATITUDE = 0.0
        private const val WORLD_CENTER_LONGITUDE = 0.0

        /** Bounding box of the whole world, used to bound panning and zoom. */
        private val WORLD_BOUNDS =
            BoundingBox(WORLD_LAT_NORTH, WORLD_LON_EAST, WORLD_LAT_SOUTH, WORLD_LON_WEST)
    }
}
