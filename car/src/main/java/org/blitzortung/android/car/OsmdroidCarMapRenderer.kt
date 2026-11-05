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
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Surface
import android.view.ViewGroup
import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import androidx.preference.PreferenceManager
import kotlin.math.max
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.overlay.OwnLocationOverlay
import org.blitzortung.android.map.overlay.StrikeListOverlay
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.protocol.MapViewport
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

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

    internal val strikeListOverlay: StrikeListOverlay =
        StrikeListOverlay(mapView, strikeColorHandler, showDataArea = false)

    internal val ownLocationOverlay: OwnLocationOverlay = OwnLocationOverlay(carContext, mapView)

    private val viewport: MapViewport = CarMapViewport(mapView)

    private var viewportListener: ((MapViewport) -> Unit)? = null

    /**
     * Dimming scrim. Added as the first overlay so it covers the map tiles only,
     * leaving the strike and grid overlays drawn after it undimmed.
     */
    internal val dimOverlay: MapDimOverlay = MapDimOverlay()

    /**
     * Strike age colour legend. Added last so it draws above the strike and
     * dimming overlays and is never dimmed by the scrim.
     */
    internal val legendOverlay: CarLegendOverlay = CarLegendOverlay(carContext, strikeListOverlay)

    init {
        // Index 0 => drawn directly above the map tiles and below every data
        // overlay (strikes, grid, own location), so only the background dims.
        mapView.overlays.add(0, dimOverlay)
        mapView.overlays.add(strikeListOverlay)
        mapView.overlays.add(ownLocationOverlay)
        mapView.overlays.add(legendOverlay)
        mapView.addMapListener(strikeListOverlay)
        mapView.addMapListener(ownLocationOverlay)

        // The viewport is meaningless before the map has a size, so the initial
        // notification (and resize notifications) come from the layout callback.
        mapView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                notifyViewportChanged()
            }
        }
    }

    override fun setViewportListener(listener: (MapViewport) -> Unit) {
        viewportListener = listener
        notifyViewportChanged()
    }

    /**
     * Reports the current viewport to the listener, unless the map has not been
     * laid out yet (a zero-sized map has no meaningful bounding box).
     */
    private fun notifyViewportChanged() {
        if (mapView.width <= 0 || mapView.height <= 0) {
            return
        }
        viewportListener?.invoke(viewport)
    }

    private val displayManager: DisplayManager =
        carContext.getSystemService(DisplayManager::class.java)

    private var virtualDisplay: VirtualDisplay? = null

    private var presentation: Presentation? = null

    /** Dimensions of the virtual display, used to resize it for a new surface. */
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var surfaceDpi = 0

    /** Whether the camera has been framed on a location fix; framing happens at most once. */
    private var initialLocationFramed = false

    /** Whether the driver has panned or zoomed, which suppresses the initial framing. */
    private var userMovedMap = false

    /** Whether a stored camera was restored, which suppresses the initial framing. */
    private var cameraRestored = false

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        legendOverlay.densityDpi = surfaceContainer.dpi

        surfaceContainer.surface?.let { surface -> showOnSurface(surfaceContainer, surface) }
    }

    /**
     * Binds the map to the host surface. The virtual display and presentation are
     * created once and reused for replacement surfaces: reusing them avoids
     * re-parenting the shared map view and keeps it alive across surface changes.
     */
    private fun showOnSurface(
        surfaceContainer: SurfaceContainer,
        surface: Surface,
    ) {
        val display = virtualDisplay
        val currentPresentation = presentation
        if (display == null || currentPresentation == null) {
            applyInitialCamera(surfaceContainer.width, surfaceContainer.height)
            createPresentation(surfaceContainer, surface)
            return
        }

        if (surfaceContainer.width != surfaceWidth ||
            surfaceContainer.height != surfaceHeight ||
            surfaceContainer.dpi != surfaceDpi
        ) {
            updateWorldBounds(surfaceContainer.width, surfaceContainer.height)
            display.resize(surfaceContainer.width, surfaceContainer.height, surfaceContainer.dpi)
            surfaceWidth = surfaceContainer.width
            surfaceHeight = surfaceContainer.height
            surfaceDpi = surfaceContainer.dpi
        }
        display.surface = surface
        if (!currentPresentation.isShowing) {
            currentPresentation.show()
        }
        mapView.invalidate()
    }

    private fun createPresentation(
        surfaceContainer: SurfaceContainer,
        surface: Surface,
    ) {
        val display =
            displayManager.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                surfaceContainer.width,
                surfaceContainer.height,
                surfaceContainer.dpi,
                surface,
                0,
            ) ?: return

        virtualDisplay = display

        val activeDisplay = display.display
        if (activeDisplay == null) {
            display.release()
            virtualDisplay = null
            return
        }

        surfaceWidth = surfaceContainer.width
        surfaceHeight = surfaceContainer.height
        surfaceDpi = surfaceContainer.dpi
        presentation =
            Presentation(carContext, activeDisplay).apply {
                setContentView(mapView)
                show()
            }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        // Keep the presentation and map view attached. The host destroys the
        // surface when the map is not visible and supplies a replacement when it
        // comes back (for example after opening the settings screen). Tearing the
        // presentation down here loses the map if the host does not re-announce
        // the surface, and a late destroy callback can race with a newer surface.
    }

    /**
     * Releases the presentation and virtual display. The renderer must not be used
     * afterwards. Detaching the map view is required because [Presentation.dismiss]
     * keeps its content view attached, which would fail when the view is added to
     * another window later.
     */
    override fun release() {
        presentation?.dismiss()
        presentation = null
        (mapView.parent as? ViewGroup)?.removeView(mapView)
        virtualDisplay?.release()
        virtualDisplay = null
        surfaceWidth = 0
        surfaceHeight = 0
        surfaceDpi = 0
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        legendOverlay.onVisibleAreaChanged(visibleArea)
        mapView.invalidate()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        legendOverlay.onStableAreaChanged(stableArea)
        mapView.invalidate()
    }

    override fun onScroll(
        distanceX: Float,
        distanceY: Float,
    ) {
        userMovedMap = true
        // The host reports gesture deltas like GestureDetector: positive values
        // mean the finger moved left/up, so the map content follows the finger.
        mapView.controller.scrollBy(distanceX.toInt(), distanceY.toInt())
        storeCamera()
        notifyViewportChanged()
    }

    override fun onFling(
        velocityX: Float,
        velocityY: Float,
    ) {
        userMovedMap = true
        // osmdroid has no public fling API; approximate it with a proportional
        // scroll in the direction of the fling.
        mapView.controller.scrollBy(
            (-velocityX / FLING_DIVISOR).toInt(),
            (-velocityY / FLING_DIVISOR).toInt(),
        )
        storeCamera()
        notifyViewportChanged()
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
        notifyViewportChanged()
    }

    override fun onClick(
        x: Float,
        y: Float,
    ) {
        // Strike details are a phone-only affordance; the car map has no popup.
    }

    override fun setContentRowCount(rowCount: Int) {
        if (legendOverlay.contentRowCount == rowCount) {
            return
        }
        legendOverlay.contentRowCount = rowCount
        mapView.invalidate()
    }

    override fun setDimmed(dimmed: Boolean) {
        dimOverlay.alpha = if (dimmed) DIM_ALPHA else 0
        mapView.invalidate()
    }

    override fun zoomIn() {
        userMovedMap = true
        mapView.controller.zoomIn()
        storeCamera()
        notifyViewportChanged()
    }

    override fun zoomOut() {
        userMovedMap = true
        mapView.controller.zoomOut()
        storeCamera()
        notifyViewportChanged()
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
        notifyViewportChanged()
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

        val worldZoom = updateWorldBounds(width, height)

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
        notifyViewportChanged()
    }

    /**
     * Updates the panning/zoom limits for the current surface size without
     * touching the camera, so a surface recreation does not reset the viewport.
     */
    private fun updateWorldBounds(
        width: Int,
        height: Int,
    ): Double {
        if (width <= 0 || height <= 0) {
            return mapView.minZoomLevel
        }
        val worldZoom = worldViewZoomFor(width, height)
        mapView.setMinZoomLevel(worldZoom)
        mapView.setScrollableAreaLimitDouble(WORLD_BOUNDS)
        return worldZoom
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
