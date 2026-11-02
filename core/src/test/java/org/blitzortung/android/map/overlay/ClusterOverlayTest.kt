package org.blitzortung.android.map.overlay

import android.graphics.Point
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.ClusterParameters
import org.blitzortung.android.data.beans.Cluster
import org.blitzortung.android.map.overlay.color.StrikeColorHandler
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClusterOverlayTest {
    private val colorHandler = mockk<StrikeColorHandler>(relaxed = true)
    private val mapView = mockk<MapView>()
    private val projection = mockk<Projection>()
    private val canvas = mockk<android.graphics.Canvas>(relaxed = true)

    private val uut = ClusterOverlay(colorHandler)

    init {
        every { mapView.projection } returns projection
        every { mapView.zoomLevelDouble } returns 5.0
        every { projection.worldMapSize } returns 720.0
        every { projection.toPixels(any(), any()) } answers {
            val geoPoint = firstArg<GeoPoint>()
            val point = secondArg<Point>()
            point.x = (geoPoint.longitude * 10).toInt()
            point.y = (geoPoint.latitude * 10).toInt()
            point
        }
    }

    @Test
    fun setClustersColorsEveryClusterAndDrawsThem() {
        uut.setClusters(clusters(), ClusterParameters(), resultReferenceTime = 1_000_000L, resultIntervalDuration = 600)

        uut.draw(canvas, mapView, false)

        verify(exactly = 1) { colorHandler.updateTarget() }
        verify(exactly = 2) { colorHandler.getColor(any()) }
        // newest cluster is filled + stroked, older one only stroked
        verify(exactly = 3) { canvas.drawPath(any(), any()) }
    }

    @Test
    fun setClustersWithoutClustersDrawsNothing() {
        uut.setClusters(emptyList(), ClusterParameters(), resultReferenceTime = 0L, resultIntervalDuration = 600)

        uut.draw(canvas, mapView, false)

        verify(exactly = 0) { canvas.drawPath(any(), any()) }
    }

    @Test
    fun clearRemovesAllShapes() {
        uut.setClusters(clusters(), ClusterParameters(), resultReferenceTime = 1_000_000L, resultIntervalDuration = 600)
        uut.clear()

        uut.draw(canvas, mapView, false)

        verify(exactly = 0) { canvas.drawPath(any(), any()) }
    }

    @Test
    fun drawSkipsShadowAndNullArguments() {
        uut.setClusters(clusters(), ClusterParameters(), resultReferenceTime = 1_000_000L, resultIntervalDuration = 600)

        uut.draw(canvas, mapView, true)
        uut.draw(null, mapView, false)
        uut.draw(canvas, null, false)

        verify(exactly = 0) { canvas.drawPath(any(), any()) }
    }

    @Test
    fun visibilityIsDelegatedToTheLayerComponent() {
        assertThat(uut.visible).isTrue()

        uut.visible = false

        assertThat(uut.visible).isFalse()
    }

    private fun clusters(): List<Cluster> =
        listOf(
            Cluster(
                id = 1L,
                timestamp = 1_000_000L,
                intervalSeconds = 600,
                strikeCount = 12,
                area = 1.0,
                shape = listOf(11.0 to 51.0, 11.1 to 51.0, 11.1 to 51.1),
            ),
            Cluster(
                id = 2L,
                timestamp = 999_400L,
                intervalSeconds = 600,
                strikeCount = 4,
                area = 0.5,
                shape = listOf(12.0 to 52.0, 12.1 to 52.0, 12.1 to 52.1),
            ),
        )
}
