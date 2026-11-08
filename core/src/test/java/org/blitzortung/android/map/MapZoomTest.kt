package org.blitzortung.android.map

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapZoomTest {
    @Test
    fun `the whole world fits at zoom zero at the equator`() {
        val worldCircumference = 2 * Math.PI * MapZoom.EARTH_RADIUS_METERS

        val zoom = MapZoom.zoomForSpan(worldCircumference, TILE_SIZE, latitude = 0.0)

        assertThat(zoom).isCloseTo(0.0, within(1e-9))
    }

    @Test
    fun `a known span yields the expected fractional zoom`() {
        // The equator is 40,075,016.686 m, so 1,000,000 m fits at log2(40.075).
        val zoom = MapZoom.zoomForSpan(1_000_000.0, TILE_SIZE, latitude = 0.0)

        assertThat(zoom).isCloseTo(5.324631215945821, within(1e-9))
    }

    @Test
    fun `the result is not rounded to a whole zoom`() {
        val zoom = MapZoom.zoomForSpan(1_000_000.0, TILE_SIZE, latitude = 0.0)

        assertThat(zoom).isNotEqualTo(zoom.toInt().toDouble())
    }

    @Test
    fun `framing the same ground distance at a higher latitude uses the Mercator scale`() {
        val atEquator = MapZoom.zoomForSpan(500_000.0, VIEWPORT_PIXELS, latitude = 0.0)
        val atBerlin = MapZoom.zoomForSpan(500_000.0, VIEWPORT_PIXELS, latitude = 52.5)

        // Mercator enlarges the ground scale near the poles, so fewer pixels are
        // needed there for the same ground distance and the zoom is lower.
        assertThat(atBerlin).isCloseTo(atEquator + kotlin.math.log2(kotlin.math.cos(Math.toRadians(52.5))), within(1e-9))
    }

    @Test
    fun `the latitude is clamped to the Mercator limit`() {
        val atLimit = MapZoom.zoomForSpan(500_000.0, VIEWPORT_PIXELS, latitude = MapZoom.MAX_MERCATOR_LATITUDE)
        val beyondLimit = MapZoom.zoomForSpan(500_000.0, VIEWPORT_PIXELS, latitude = 90.0)
        val southOfLimit = MapZoom.zoomForSpan(500_000.0, VIEWPORT_PIXELS, latitude = -90.0)

        assertThat(beyondLimit).isCloseTo(atLimit, within(1e-9))
        assertThat(beyondLimit).isFinite()
        assertThat(southOfLimit).isCloseTo(atLimit, within(1e-9))
    }

    @Test
    fun `a wider viewport needs a higher zoom for the same span`() {
        val narrow = MapZoom.zoomForSpan(500_000.0, 400, latitude = 50.0)
        val wide = MapZoom.zoomForSpan(500_000.0, 800, latitude = 50.0)

        assertThat(wide).isCloseTo(narrow + 1.0, within(1e-9))
    }

    private companion object {
        private const val TILE_SIZE = 256
        private const val VIEWPORT_PIXELS = 480
    }
}
