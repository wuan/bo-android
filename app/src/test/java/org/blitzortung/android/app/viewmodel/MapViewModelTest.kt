package org.blitzortung.android.app.viewmodel

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.osmdroid.util.GeoPoint

class MapViewModelTest {
    private lateinit var uut: MapViewModel

    @Before
    fun setUp() {
        uut = MapViewModel()
    }

    @Test
    fun updatesZoomLevel() {
        uut.updateZoomLevel(7.5)

        assertThat(uut.zoomLevel.value).isEqualTo(7.5)
    }

    @Test
    fun updatesCenterPosition() {
        val position = GeoPoint(52.0, 13.0)

        uut.updateCenterPosition(position)

        assertThat(uut.centerPosition.value).isSameAs(position)
    }

    @Test
    fun updatesMapTypeAndReadyState() {
        uut.updateMapType("OpenTopoMap")
        uut.setMapReady(true)

        assertThat(uut.mapType.value).isEqualTo("OpenTopoMap")
        assertThat(uut.isMapReady.value).isTrue()
    }

    @Test
    fun savesMapState() {
        val position = GeoPoint(52.0, 13.0)

        uut.saveMapState(9.0, position)

        assertThat(uut.zoomLevel.value).isEqualTo(9.0)
        assertThat(uut.centerPosition.value).isSameAs(position)
    }
}
