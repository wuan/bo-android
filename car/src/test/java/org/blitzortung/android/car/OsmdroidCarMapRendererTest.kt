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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.location.Location
import android.widget.FrameLayout
import androidx.car.app.SurfaceContainer
import androidx.car.app.testing.TestCarContext
import androidx.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridElement
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.location.LocationUpdate
import org.blitzortung.android.location.NoLocation
import org.blitzortung.android.map.overlay.color.ColorTarget
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.blitzortung.android.protocol.MapViewport
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.TilesOverlay
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OsmdroidCarMapRendererTest {
    private fun createRenderer(
        settings: CarSettings = CarSettings(context()),
    ): OsmdroidCarMapRenderer {
        val context = context()
        val carContext = TestCarContext.createCarContext(context)
        val colorHandler = StrikeColorHandler(PreferenceManager.getDefaultSharedPreferences(context))
        return OsmdroidCarMapRenderer(carContext, colorHandler, settings)
    }

    private fun context() = RuntimeEnvironment.getApplication()

    @Test
    fun `projects the map centre to the middle of the viewport`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE))

        val point = renderer.screenPositionOf(CENTER_LATITUDE, CENTER_LONGITUDE)

        assertThat(point.x).isCloseTo(VIEWPORT_WIDTH / 2, within(2))
        assertThat(point.y).isCloseTo(VIEWPORT_HEIGHT / 2, within(2))
    }

    @Test
    fun `reuses the shared strike colour handler`() {
        val context = RuntimeEnvironment.getApplication()
        val carContext = TestCarContext.createCarContext(context)
        val colorHandler = StrikeColorHandler(PreferenceManager.getDefaultSharedPreferences(context))

        val renderer = OsmdroidCarMapRenderer(carContext, colorHandler, CarSettings(context))

        assertThat(renderer.strikeListOverlay.colorHandler).isSameAs(colorHandler)
    }

    @Test
    fun `the scrim is transparent before any shading is applied`() {
        val renderer = createRenderer()

        assertThat(renderer.shadeOverlay.alpha).isEqualTo(0)
    }

    @Test
    fun `dark shading draws a black scrim over the map`() {
        val renderer = createRenderer()

        renderer.setShading(MapShading.DARK)

        assertThat(renderer.shadeOverlay.color).isEqualTo(Color.BLACK)
        assertThat(renderer.shadeOverlay.alpha).isEqualTo(MapShadeOverlay.DARK_ALPHA)
    }

    @Test
    fun `bright shading draws a white scrim over the map`() {
        val renderer = createRenderer()

        renderer.setShading(MapShading.BRIGHT)

        assertThat(renderer.shadeOverlay.color).isEqualTo(Color.WHITE)
        assertThat(renderer.shadeOverlay.alpha).isEqualTo(MapShadeOverlay.BRIGHT_ALPHA)
    }

    @Test
    fun `dark shading keeps the bright strike palette`() {
        val renderer = createRenderer()

        renderer.setShading(MapShading.DARK)

        assertThat(renderer.strikeListOverlay.colorHandler.colors)
            .isEqualTo(renderer.strikeListOverlay.colorHandler.getColors(ColorTarget.SATELLITE))
    }

    @Test
    fun `bright shading darkens the strike palette`() {
        val renderer = createRenderer()

        renderer.setShading(MapShading.BRIGHT)

        assertThat(renderer.strikeListOverlay.colorHandler.colors)
            .isEqualTo(renderer.strikeListOverlay.colorHandler.getColors(ColorTarget.STREETMAP))
    }

    @Test
    fun `changing the shading recolours the strikes`() {
        val renderer = createRenderer()
        renderer.setShading(MapShading.DARK)
        val darkPalette = renderer.strikeListOverlay.colorHandler.colors

        renderer.setShading(MapShading.BRIGHT)

        assertThat(renderer.strikeListOverlay.colorHandler.colors).isNotEqualTo(darkPalette)
    }

    @Test
    fun `the shade overlay is drawn beneath the strike overlay`() {
        val renderer = createRenderer()

        val overlays = renderer.mapView.overlays

        assertThat(overlays.indexOf(renderer.shadeOverlay)).isEqualTo(0)
        assertThat(overlays.indexOf(renderer.shadeOverlay)).isLessThan(overlays.indexOf(renderer.strikeListOverlay))
    }

    @Test
    fun `overlays drawn after the shade overlay are not shaded`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        val magenta = Paint().apply { color = Color.MAGENTA }
        renderer.mapView.overlays.add(
            object : Overlay() {
                override fun draw(
                    canvas: Canvas?,
                    mapView: MapView?,
                    shadow: Boolean,
                ) {
                    if (!shadow) {
                        canvas?.drawRect(canvas.clipBounds, magenta)
                    }
                }
            },
        )
        renderer.setShading(MapShading.DARK)

        val bitmap = Bitmap.createBitmap(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, Bitmap.Config.ARGB_8888)
        renderer.mapView.overlayManager.onDraw(Canvas(bitmap), renderer.mapView)

        assertThat(bitmap.getPixel(VIEWPORT_WIDTH / 2, VIEWPORT_HEIGHT / 2)).isEqualTo(Color.MAGENTA)
    }

    @Test
    fun `the refresh countdown is drawn by the legend panel`() {
        val renderer = createRenderer()

        renderer.setCountdown("40 / 60s")

        assertThat(renderer.legendOverlay.countdown).isEqualTo("40 / 60s")
    }

    @Test
    fun `the legend overlay is present and drawn last`() {
        val renderer = createRenderer()
        val overlays = renderer.mapView.overlays

        assertThat(overlays).contains(renderer.legendOverlay)
        assertThat(overlays.indexOf(renderer.legendOverlay)).isEqualTo(overlays.size - 1)
    }

    @Test
    fun `the legend is drawn above the shade, strike and location overlays`() {
        val renderer = createRenderer()
        val overlays = renderer.mapView.overlays
        val legendIndex = overlays.indexOf(renderer.legendOverlay)

        assertThat(legendIndex)
            .describedAs("the legend must be drawn after the scrim so map shading cannot affect it")
            .isGreaterThan(overlays.indexOf(renderer.shadeOverlay))
        assertThat(legendIndex).isGreaterThan(overlays.indexOf(renderer.strikeListOverlay))
        assertThat(legendIndex).isGreaterThan(overlays.indexOf(renderer.ownLocationOverlay))
    }

    @Test
    fun `the legend sits at the bottom-left of the host's map area`() {
        val renderer = createRenderer()

        renderer.onVisibleAreaChanged(Rect(320, 80, VIEWPORT_WIDTH, VIEWPORT_HEIGHT - 40))

        val bounds = renderer.legendOverlay.panelBounds()
        assertThat(bounds.left).isLessThan(100f)
        assertThat(bounds.bottom).isLessThanOrEqualTo(VIEWPORT_HEIGHT - 40f)
    }

    @Test
    fun `the legend stays above the host's map area bottom once the surface height is known`() {
        val renderer = createRenderer()
        renderer.legendOverlay.surfaceHeight = VIEWPORT_HEIGHT
        val areaBottom = VIEWPORT_HEIGHT - 200

        renderer.onStableAreaChanged(Rect(378, 123, 734, areaBottom))

        val bounds = renderer.legendOverlay.panelBounds()
        assertThat(bounds.bottom).isLessThanOrEqualTo(areaBottom.toFloat())
    }

    @Test
    fun `the legend falls back to the surface bottom while no map area is known`() {
        val renderer = createRenderer()
        renderer.legendOverlay.surfaceHeight = VIEWPORT_HEIGHT

        val bounds = renderer.legendOverlay.panelBounds()

        assertThat(bounds.bottom).isLessThanOrEqualTo(VIEWPORT_HEIGHT.toFloat())
        assertThat(bounds.bottom).isGreaterThan(VIEWPORT_HEIGHT - 100f)
    }

    @Test
    fun `shading does not invert the map tiles`() {
        val renderer = createRenderer()

        renderer.setShading(MapShading.DARK)

        val colorFilterField = TilesOverlay::class.java.getDeclaredField("currentColorFilter")
        colorFilterField.isAccessible = true
        assertThat(colorFilterField.get(renderer.mapView.overlayManager.tilesOverlay)).isNull()
    }

    @Test
    fun `projects north of the centre above it`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE))

        val centre = renderer.screenPositionOf(CENTER_LATITUDE, CENTER_LONGITUDE)
        val north = renderer.screenPositionOf(CENTER_LATITUDE + 1.0, CENTER_LONGITUDE)
        val east = renderer.screenPositionOf(CENTER_LATITUDE, CENTER_LONGITUDE + 1.0)

        assertThat(north.y).isLessThan(centre.y)
        assertThat(east.x).isGreaterThan(centre.x)
    }

    @Test
    fun `frames the map on the first location fix`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(OsmdroidCarMapRenderer.DEFAULT_ZOOM_LEVEL)
    }

    @Test
    fun `does not reframe after the map has framed the location`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))
        renderer.onScroll(-SCROLL_DISTANCE, -SCROLL_DISTANCE)

        val centerAfterScroll = GeoPoint(renderer.mapView.mapCenter.latitude, renderer.mapView.mapCenter.longitude)
        val zoomAfterScroll = renderer.mapView.zoomLevelDouble

        renderer.onLocationEvent(fix(FIX_LATITUDE + 5.0, FIX_LONGITUDE + 5.0))

        assertThat(renderer.mapView.mapCenter.latitude).isCloseTo(centerAfterScroll.latitude, within(0.001))
        assertThat(renderer.mapView.mapCenter.longitude).isCloseTo(centerAfterScroll.longitude, within(0.001))
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(zoomAfterScroll)
    }

    @Test
    fun `frames only once even without user interaction`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        renderer.onLocationEvent(fix(FIX_LATITUDE + 5.0, FIX_LONGITUDE + 5.0))

        assertCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
    }

    @Test
    fun `does not frame when the user moved the map before the first fix`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(INITIAL_ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))
        renderer.onScroll(-SCROLL_DISTANCE, -SCROLL_DISTANCE)

        val centerBefore = GeoPoint(renderer.mapView.mapCenter.latitude, renderer.mapView.mapCenter.longitude)

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertThat(renderer.mapView.mapCenter.latitude).isCloseTo(centerBefore.latitude, within(0.001))
        assertThat(renderer.mapView.mapCenter.longitude).isCloseTo(centerBefore.longitude, within(0.001))
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(INITIAL_ZOOM)
    }

    @Test
    fun `does not frame when the action strip zoom is used before the first fix`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))
        renderer.zoomIn()

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertNotCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
    }

    @Test
    fun `leaves the camera unchanged when no location is available`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(INITIAL_ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))

        renderer.onLocationEvent(NoLocation)

        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(INITIAL_ZOOM)
        assertCentredOn(renderer, INITIAL_LATITUDE, INITIAL_LONGITUDE)
    }

    @Test
    fun `frames on the first fix that follows a no-location event`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(NoLocation)

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(OsmdroidCarMapRenderer.DEFAULT_ZOOM_LEVEL)
    }

    @Test
    fun `world view covers the viewport`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        val worldPixels = TileSystem.getTileSize() * 2.0.pow(renderer.mapView.zoomLevelDouble)
        assertThat(worldPixels).isGreaterThanOrEqualTo(max(VIEWPORT_WIDTH, VIEWPORT_HEIGHT).toDouble())
        assertCentredOn(renderer, 0.0, 0.0)
    }

    @Test
    fun `zooming out stops at the world view`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        val floor = renderer.mapView.zoomLevelDouble

        repeat(5) { renderer.zoomOut() }

        assertThat(renderer.mapView.zoomLevelDouble).isGreaterThanOrEqualTo(floor)
        assertThat(renderer.mapView.canZoomOut()).isFalse()
    }

    @Test
    fun `dragging the map moves it with the finger`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(CENTER_LATITUDE, CENTER_LONGITUDE))

        // A drag to the left and up reports positive distances; the map content
        // must follow the finger, so the centre moves east and south.
        renderer.onScroll(SCROLL_DISTANCE, SCROLL_DISTANCE)

        assertThat(renderer.mapView.mapCenter.longitude).isGreaterThan(CENTER_LONGITUDE)
        assertThat(renderer.mapView.mapCenter.latitude).isLessThan(CENTER_LATITUDE)
    }

    @Test
    fun `panning beyond the world edge is clamped to the bounds`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(8.0)

        renderer.onScroll(0f, -200_000f)

        val latitude = renderer.mapView.mapCenter.latitude
        assertThat(latitude).isBetween(-85.06, 85.06)
        assertThat(abs(latitude))
            .describedAs("the map should have reached and stopped at a world edge")
            .isGreaterThan(84.0)
    }

    @Test
    fun `a wider surface yields a higher world view zoom`() {
        val renderer = createRenderer()

        renderer.mapView.layout(0, 0, 1000, 400)
        renderer.applyInitialCamera(1000, 400)
        val wideZoom = renderer.mapView.zoomLevelDouble

        renderer.mapView.layout(0, 0, 400, 400)
        renderer.applyInitialCamera(400, 400)
        val narrowZoom = renderer.mapView.zoomLevelDouble

        assertThat(wideZoom).isGreaterThan(narrowZoom)
    }

    @Test
    fun `restores a stored camera instead of the world view`() {
        val settings = CarSettings(context())
        settings.storeCamera(CarCamera(latitude = 52.0, longitude = 13.0, zoom = 9.0))
        val renderer = createRenderer(settings)
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(9.0)
        assertCentredOn(renderer, 52.0, 13.0)
    }

    @Test
    fun `a stored zoom below the world view is raised to the floor`() {
        val settings = CarSettings(context())
        settings.storeCamera(CarCamera(latitude = 0.0, longitude = 0.0, zoom = 0.5))
        val renderer = createRenderer(settings)
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        assertThat(renderer.mapView.zoomLevelDouble)
            .isGreaterThanOrEqualTo(renderer.worldViewZoomFor(VIEWPORT_WIDTH, VIEWPORT_HEIGHT))
    }

    @Test
    fun `a restored camera suppresses first-fix framing`() {
        val settings = CarSettings(context())
        settings.storeCamera(CarCamera(latitude = 52.0, longitude = 13.0, zoom = 6.0))
        val renderer = createRenderer(settings)
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))

        assertCentredOn(renderer, 52.0, 13.0)
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(6.0)
    }

    @Test
    fun `a destroyed surface keeps the map view attached for the next surface`() {
        val renderer = createRenderer()
        val presentationRoot = FrameLayout(context())
        presentationRoot.addView(renderer.mapView)

        renderer.onSurfaceDestroyed(SurfaceContainer(null, VIEWPORT_WIDTH, VIEWPORT_HEIGHT, 160))

        assertThat(renderer.mapView.parent).isSameAs(presentationRoot)
    }

    @Test
    fun `releasing the renderer detaches the map view`() {
        val renderer = createRenderer()
        val presentationRoot = FrameLayout(context())
        presentationRoot.addView(renderer.mapView)

        renderer.release()

        assertThat(renderer.mapView.parent).isNull()
    }

    @Test
    fun `panning stores the camera`() {
        val settings = CarSettings(context())
        val renderer = createRenderer(settings)
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(6.0)

        renderer.onScroll(-SCROLL_DISTANCE, -SCROLL_DISTANCE)

        val stored = settings.camera
        assertThat(stored).isNotNull
        assertThat(stored!!.zoom).isEqualTo(6.0)
        assertThat(stored.latitude).isCloseTo(renderer.mapView.mapCenter.latitude, within(0.001))
        assertThat(stored.longitude).isCloseTo(renderer.mapView.mapCenter.longitude, within(0.001))
    }

    @Test
    fun `centres and zooms on the current location`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))
        renderer.mapView.controller.setZoom(INITIAL_ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))

        renderer.goToCurrentLocation()

        assertCentredOn(renderer, FIX_LATITUDE, FIX_LONGITUDE)
        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(OsmdroidCarMapRenderer.DEFAULT_ZOOM_LEVEL)
    }

    @Test
    fun `the location action does nothing without a location fix`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.mapView.controller.setZoom(INITIAL_ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))

        renderer.goToCurrentLocation()

        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(INITIAL_ZOOM)
        assertCentredOn(renderer, INITIAL_LATITUDE, INITIAL_LONGITUDE)
    }

    @Test
    fun `the location action does nothing after the location is lost`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.onLocationEvent(fix(FIX_LATITUDE, FIX_LONGITUDE))
        renderer.onLocationEvent(NoLocation)
        renderer.mapView.controller.setZoom(INITIAL_ZOOM)
        renderer.mapView.controller.setCenter(GeoPoint(INITIAL_LATITUDE, INITIAL_LONGITUDE))

        renderer.goToCurrentLocation()

        assertThat(renderer.mapView.zoomLevelDouble).isEqualTo(INITIAL_ZOOM)
        assertCentredOn(renderer, INITIAL_LATITUDE, INITIAL_LONGITUDE)
    }

    @Test
    fun `zooming stores the camera`() {
        val settings = CarSettings(context())
        val renderer = createRenderer(settings)
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        renderer.zoomIn()

        val stored = settings.camera
        assertThat(stored).isNotNull
        assertThat(stored!!.zoom).isCloseTo(renderer.mapView.zoomLevelDouble, within(0.001))
    }

    @Test
    fun `notifies the viewport listener after a programmatic scroll`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        val viewports = mutableListOf<MapViewport>()
        renderer.setViewportListener { viewports.add(it) }
        viewports.clear()

        renderer.onScroll(-SCROLL_DISTANCE, -SCROLL_DISTANCE)

        assertThat(viewports).isNotEmpty()
        assertThat(viewports.last().viewportBoundingBox).isNotNull
    }

    @Test
    fun `notifies the viewport listener after a zoom`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.applyInitialCamera(VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        val viewports = mutableListOf<MapViewport>()
        renderer.setViewportListener { viewports.add(it) }
        viewports.clear()

        renderer.zoomIn()

        assertThat(viewports).isNotEmpty()
        assertThat(viewports.last().viewportZoomLevel).isEqualTo(renderer.mapView.zoomLevelDouble)
    }

    @Test
    fun `does not report a viewport before the map is laid out`() {
        val renderer = createRenderer()
        val viewports = mutableListOf<MapViewport>()

        renderer.setViewportListener { viewports.add(it) }

        assertThat(viewports).isEmpty()
    }

    @Test
    fun `the car strike overlay does not draw the data-area outline`() {
        val renderer = createRenderer()
        renderer.mapView.layout(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)
        renderer.strikeListOverlay.parameters = Parameters(region = LOCAL_REGION)
        renderer.strikeListOverlay.gridParameters =
            GridParameters(
                longitudeStart = 9.0,
                latitudeStart = 51.0,
                longitudeDelta = 1.0,
                latitudeDelta = 1.0,
                longitudeBins = 2,
                latitudeBins = 2,
            )
        val canvas = mockk<Canvas>(relaxed = true)
        every { canvas.clipBounds } returns Rect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT)

        assertThat(renderer.strikeListOverlay.showDataArea).isFalse()

        renderer.strikeListOverlay.draw(canvas, renderer.mapView, false)

        verify(exactly = 0) { canvas.drawLine(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `strikes received before shading use the default palette`() {
        val renderer = createRenderer()

        renderer.updateStrikes(strikeEvent(updated = -1))

        assertThat(renderer.strikeListOverlay.colorHandler.colors)
            .isEqualTo(renderer.strikeListOverlay.colorHandler.getColors(ColorTarget.SATELLITE))
    }

    @Test
    fun `strikes received after bright shading use the darker palette`() {
        val renderer = createRenderer()
        renderer.setShading(MapShading.BRIGHT)

        renderer.updateStrikes(strikeEvent(updated = -1))

        assertThat(renderer.strikeListOverlay.colorHandler.colors)
            .isEqualTo(renderer.strikeListOverlay.colorHandler.getColors(ColorTarget.STREETMAP))
    }

    @Test
    fun `a shading change recolours strikes on the next update`() {
        val renderer = createRenderer()
        renderer.setShading(MapShading.DARK)
        renderer.updateStrikes(strikeEvent(updated = -1))

        renderer.setShading(MapShading.BRIGHT)
        renderer.updateStrikes(strikeEvent(updated = 1))

        assertThat(renderer.strikeListOverlay.colorHandler.colors)
            .isEqualTo(renderer.strikeListOverlay.colorHandler.getColors(ColorTarget.STREETMAP))
    }

    private fun strikeEvent(updated: Int): DataReceived =
        DataReceived(
            strikes = listOf(GridElement(REFERENCE_TIME, 10.0, 50.0, 1)),
            referenceTime = REFERENCE_TIME,
            updated = updated,
            parameters = Parameters(),
            flags = Flags(),
        )

    private fun assertCentredOn(
        renderer: OsmdroidCarMapRenderer,
        latitude: Double,
        longitude: Double,
    ) {
        val point = renderer.screenPositionOf(latitude, longitude)
        assertThat(point.x).isCloseTo(VIEWPORT_WIDTH / 2, within(2))
        assertThat(point.y).isCloseTo(VIEWPORT_HEIGHT / 2, within(2))
    }

    private fun assertNotCentredOn(
        renderer: OsmdroidCarMapRenderer,
        latitude: Double,
        longitude: Double,
    ) {
        val point = renderer.screenPositionOf(latitude, longitude)
        val offCentre =
            abs(point.x - VIEWPORT_WIDTH / 2) > 5 || abs(point.y - VIEWPORT_HEIGHT / 2) > 5
        assertThat(offCentre).describedAs("coordinate should not be framed at the viewport centre").isTrue()
    }

    private fun fix(
        latitude: Double,
        longitude: Double,
    ): LocationUpdate =
        LocationUpdate(
            Location("test").apply {
                this.latitude = latitude
                this.longitude = longitude
            },
        )

    private companion object {
        private const val VIEWPORT_WIDTH = 800
        private const val VIEWPORT_HEIGHT = 480
        private const val ZOOM = 8.0
        private const val CENTER_LATITUDE = 50.0
        private const val CENTER_LONGITUDE = 10.0
        private const val FIX_LATITUDE = 48.5
        private const val FIX_LONGITUDE = 9.0
        private const val INITIAL_ZOOM = 4.0
        private const val INITIAL_LATITUDE = 52.0
        private const val INITIAL_LONGITUDE = 13.0
        private const val SCROLL_DISTANCE = 50f
        private const val LOCAL_REGION = -1
        private const val REFERENCE_TIME = 1_000_000L
    }
}
