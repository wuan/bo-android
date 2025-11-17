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

package org.blitzortung.android.app.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.osmdroid.util.GeoPoint

class MapViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        viewModel = MapViewModel()
    }

    @Test
    fun `initial zoom level is 3_0`() {
        assertThat(viewModel.zoomLevel.value).isEqualTo(3.0)
    }

    @Test
    fun `initial center position is null`() {
        assertThat(viewModel.centerPosition.value).isNull()
    }

    @Test
    fun `initial map type is OpenStreetMap`() {
        assertThat(viewModel.mapType.value).isEqualTo("OpenStreetMap")
    }

    @Test
    fun `initial map ready state is false`() {
        assertThat(viewModel.isMapReady.value).isFalse()
    }

    @Test
    fun `updateZoomLevel updates zoom level state`() {
        viewModel.updateZoomLevel(5.5)

        assertThat(viewModel.zoomLevel.value).isEqualTo(5.5)
    }

    @Test
    fun `updateCenterPosition updates center position state`() {
        val geoPoint = GeoPoint(48.0, 11.0)

        viewModel.updateCenterPosition(geoPoint)

        assertThat(viewModel.centerPosition.value).isEqualTo(geoPoint)
    }

    @Test
    fun `updateMapType updates map type state`() {
        viewModel.updateMapType("Satellite")

        assertThat(viewModel.mapType.value).isEqualTo("Satellite")
    }

    @Test
    fun `setMapReady updates map ready state`() {
        viewModel.setMapReady(true)

        assertThat(viewModel.isMapReady.value).isTrue()

        viewModel.setMapReady(false)

        assertThat(viewModel.isMapReady.value).isFalse()
    }

    @Test
    fun `saveMapState updates both zoom and center`() {
        val zoom = 7.0
        val center = GeoPoint(52.0, 13.0)

        viewModel.saveMapState(zoom, center)

        assertThat(viewModel.zoomLevel.value).isEqualTo(zoom)
        assertThat(viewModel.centerPosition.value).isEqualTo(center)
    }
}
