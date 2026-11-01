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

import android.graphics.Point
import android.graphics.Rect
import androidx.car.app.SurfaceContainer
import org.blitzortung.android.data.provider.result.DataReceived
import org.blitzortung.android.location.LocationEvent

/**
 * Draws the lightning map onto the surface provided by the car host. Rendering
 * and the map interactions are kept behind this interface so the screen does not
 * depend on the concrete map implementation.
 */
interface CarMapRenderer {
    fun onSurfaceAvailable(surfaceContainer: SurfaceContainer)

    fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer)

    /**
     * The part of the surface the host currently guarantees to be visible, in
     * surface pixels. Used to keep surface-drawn UI clear of host chrome such as
     * the content list.
     */
    fun onVisibleAreaChanged(visibleArea: Rect)

    /**
     * The part of the surface the host designates as stable, in surface pixels.
     * Preferred over the visible area for persistent UI so it does not move when
     * transient host UI appears.
     */
    fun onStableAreaChanged(stableArea: Rect)

    fun onScroll(
        distanceX: Float,
        distanceY: Float,
    )

    fun onFling(
        velocityX: Float,
        velocityY: Float,
    )

    fun onScale(
        focusX: Float,
        focusY: Float,
        scaleFactor: Float,
    )

    fun onClick(
        x: Float,
        y: Float,
    )

    fun setDimmed(dimmed: Boolean)

    fun zoomIn()

    fun zoomOut()

    fun updateStrikes(event: DataReceived)

    fun clearStrikes()

    fun onLocationEvent(event: LocationEvent)

    /** Screen position of a coordinate on the current map viewport. */
    fun screenPositionOf(
        latitude: Double,
        longitude: Double,
    ): Point
}
