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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.map.OwnMapView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RegionViewTest {
    private lateinit var regionView: RegionView
    private lateinit var canvas: Canvas

    private val gridParameters =
        GridParameters(
            longitudeStart = 0.0,
            latitudeStart = 0.0,
            longitudeDelta = 1.0,
            latitudeDelta = 1.0,
            longitudeBins = 10,
            latitudeBins = 10,
            size = 10000,
        )

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        regionView = RegionView(context)
        regionView.layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        regionView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        regionView.layout(0, 0, 400, 400)
        canvas = Canvas(Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888))
    }

    private fun receivedData(gridParameters: GridParameters?): DataReceived =
        DataReceived(
            gridParameters = gridParameters,
            parameters = Parameters(),
            flags = Flags(),
        )

    @Test
    fun onDrawWithoutGridParametersDoesNotThrow() {
        regionView.draw(canvas)
    }

    @Test
    fun onDrawWithGridParametersDrawsRegion() {
        regionView.dataConsumer.invoke(receivedData(gridParameters))

        regionView.draw(canvas)
    }

    @Test
    fun onDrawAfterFailedDataClearsRegion() {
        regionView.dataConsumer.invoke(receivedData(gridParameters))
        regionView.dataConsumer.invoke(receivedData(gridParameters).copy(failed = true))

        regionView.draw(canvas)
    }

    @Test
    fun onScrollUpdatesMapAreaAndZoom() {
        val mapView = mockk<OwnMapView>()
        every { mapView.boundingBox } returns BoundingBox(10.0, 0.0, 0.0, 10.0)
        every { mapView.zoomLevelDouble } returns 6.0

        regionView.dataConsumer.invoke(receivedData(gridParameters))
        val scrolled = regionView.onScroll(ScrollEvent(mapView, 100, 100))

        assertThat(scrolled).isTrue

        regionView.draw(canvas)
    }

    @Test
    fun onZoomUpdatesMapAreaAndZoom() {
        val mapView = mockk<OwnMapView>()
        every { mapView.boundingBox } returns BoundingBox(10.0, 0.0, 0.0, 10.0)

        regionView.dataConsumer.invoke(receivedData(gridParameters))
        val zoomed = regionView.onZoom(ZoomEvent(mapView, 7.0))

        assertThat(zoomed).isTrue

        regionView.draw(canvas)
    }

    @Test
    fun measureWithGridParameters() {
        regionView.dataConsumer.invoke(receivedData(gridParameters))

        regionView.measure(0, 0)

        assertThat(regionView.measuredWidth).isGreaterThanOrEqualTo(0)
    }
}
