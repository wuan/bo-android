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

import android.graphics.Rect
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CarMapSurfaceCallbackTest {
    private val renderer = mockk<CarMapRenderer>(relaxed = true)
    private val callback = CarMapSurfaceCallback(renderer)

    @Test
    fun `forwards the visible area to the renderer`() {
        val visibleArea = Rect(10, 20, 300, 400)

        callback.onVisibleAreaChanged(visibleArea)

        verify { renderer.onVisibleAreaChanged(visibleArea) }
    }

    @Test
    fun `forwards the stable area to the renderer`() {
        val stableArea = Rect(10, 20, 300, 400)

        callback.onStableAreaChanged(stableArea)

        verify { renderer.onStableAreaChanged(stableArea) }
    }
}
