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

import android.animation.ValueAnimator
import org.blitzortung.android.map.MapCameraAnimator
import org.blitzortung.android.protocol.MapViewport
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

/**
 * View-agnostic [MapViewport] adapter over the car's plain osmdroid [MapView].
 *
 * The renderer animates the camera for the location action, so the reported
 * animation state follows the underlying map. The data layer can then defer a
 * viewport-driven request until an in-flight camera transition has settled.
 */
internal class CarMapViewport(
    private val mapView: MapView,
) : MapViewport {
    override val viewportZoomLevel: Double
        get() = mapView.zoomLevelDouble

    override val viewportBoundingBox: BoundingBox?
        get() = mapView.boundingBox

    override val viewportIsAnimating: Boolean
        get() = mapView.isAnimating

    override fun animator(): ValueAnimator? = MapCameraAnimator.current(mapView)
}
