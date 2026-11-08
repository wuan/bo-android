package org.blitzortung.android.map

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.GLOBAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistogramZoomTest {
    @Test
    fun `a non-global result first frames its local data area`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = LOCAL_REGION),
                gridParameters = dataArea(),
                currentZoom = WIDE_ZOOM,
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target.longitude).isEqualTo(DATA_AREA_CENTER_LONGITUDE)
        assertThat(target.latitude).isEqualTo(DATA_AREA_CENTER_LATITUDE)
        assertThat(target.zoomLevel).isEqualTo(dataAreaZoom(HistogramZoom.LOCAL_DATA_AREA_SPAN_METERS))
    }

    @Test
    fun `tapping again at the data-area framing goes global`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = LOCAL_REGION),
                gridParameters = dataArea(),
                currentZoom = dataAreaZoom(HistogramZoom.LOCAL_DATA_AREA_SPAN_METERS),
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target).isEqualTo(globalTarget())
    }

    @Test
    fun `a fixed region frames its region-size data area`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = FIXED_REGION),
                gridParameters = dataArea(),
                currentZoom = WIDE_ZOOM,
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target.zoomLevel).isEqualTo(dataAreaZoom(HistogramZoom.REGION_DATA_AREA_SPAN_METERS))
        assertThat(target.longitude).isEqualTo(DATA_AREA_CENTER_LONGITUDE)
        assertThat(target.latitude).isEqualTo(DATA_AREA_CENTER_LATITUDE)
    }

    @Test
    fun `a view slightly wider than the data area goes global`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = LOCAL_REGION),
                gridParameters = dataArea(),
                currentZoom = dataAreaZoom(HistogramZoom.LOCAL_DATA_AREA_SPAN_METERS) - 0.75,
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target).isEqualTo(globalTarget())
    }

    @Test
    fun `a view zoomed in on the data area zooms out to its boundary first`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = LOCAL_REGION),
                gridParameters = dataArea(),
                currentZoom = dataAreaZoom(HistogramZoom.LOCAL_DATA_AREA_SPAN_METERS) + 2.0,
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target.zoomLevel).isEqualTo(dataAreaZoom(HistogramZoom.LOCAL_DATA_AREA_SPAN_METERS))
    }

    @Test
    fun `a global result goes to the global view`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = GLOBAL_REGION),
                gridParameters = dataArea(),
                currentZoom = dataAreaZoom(HistogramZoom.LOCAL_DATA_AREA_SPAN_METERS),
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target).isEqualTo(globalTarget())
    }

    @Test
    fun `the global view uses a fixed zoom level`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = GLOBAL_REGION),
                gridParameters = null,
                currentZoom = WIDE_ZOOM,
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target.zoomLevel).isEqualTo(2.5)
    }

    @Test
    fun `a global grid goes to the global view`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = LOCAL_REGION),
                gridParameters = globalGrid(),
                currentZoom = WIDE_ZOOM,
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target).isEqualTo(globalTarget())
    }

    @Test
    fun `no grid goes to the global view`() {
        val target =
            HistogramZoom.target(
                parameters = Parameters(region = LOCAL_REGION),
                gridParameters = null,
                currentZoom = WIDE_ZOOM,
                viewportPixels = VIEWPORT_PIXELS,
            )

        assertThat(target).isEqualTo(globalTarget())
    }

    private fun dataAreaZoom(spanMeters: Float): Double =
        MapZoom.zoomForSpan(
            spanMeters = spanMeters.toDouble(),
            viewportPixels = VIEWPORT_PIXELS,
            latitude = DATA_AREA_CENTER_LATITUDE,
        )

    private fun globalTarget() =
        HistogramZoomTarget(
            longitude = HistogramZoom.GLOBAL_LONGITUDE,
            latitude = HistogramZoom.GLOBAL_LATITUDE,
            zoomLevel = HistogramZoom.GLOBAL_ZOOM_LEVEL,
        )

    private fun dataArea(): GridParameters =
        GridParameters(
            longitudeStart = 9.0,
            latitudeStart = 51.0,
            longitudeDelta = 1.0,
            latitudeDelta = 1.0,
            longitudeBins = 2,
            latitudeBins = 2,
        )

    private fun globalGrid(): GridParameters =
        GridParameters(
            longitudeStart = -180.0,
            latitudeStart = 85.0,
            longitudeDelta = 1.0,
            latitudeDelta = 1.0,
            longitudeBins = 360,
            latitudeBins = 171,
        )

    private companion object {
        private const val VIEWPORT_PIXELS = 1080
        private const val WIDE_ZOOM = 2.0
        private const val FIXED_REGION = 3
        private const val DATA_AREA_CENTER_LONGITUDE = 10.0
        private const val DATA_AREA_CENTER_LATITUDE = 50.0
    }
}
