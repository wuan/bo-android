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
import org.blitzortung.android.protocol.MapViewport
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

/**
 * View-agnostic [MapViewport] adapter over the car's plain osmdroid [MapView].
 *
 * The car's camera is moved programmatically by the renderer and the host
 * surface callback rather than by map animations, so there is never an
 * in-flight camera animation: [viewportIsAnimating] is always `false` and
 * [animator] always returns `null`. The data layer therefore applies a viewport
 * change at once instead of waiting for an animation to end.
 */
internal class CarMapViewport(
    private val mapView: MapView,
) : MapViewport {
    override val viewportZoomLevel: Double
        get() = mapView.zoomLevelDouble

    override val viewportBoundingBox: BoundingBox?
        get() = mapView.boundingBox

    override val viewportIsAnimating: Boolean
        get() = false

    override fun animator(): ValueAnimator? = null
}
