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

package org.blitzortung.android.protocol

import android.animation.ValueAnimator
import org.osmdroid.util.BoundingBox

/**
 * View-agnostic port for the parts of a map view that the data layer needs to
 * derive grid parameters. Implemented by the map UI, so that
 * [org.blitzortung.android.data.MainDataHandler] does not depend on a concrete
 * `MapView` subclass.
 */
interface MapViewport {
    val viewportZoomLevel: Double

    val viewportBoundingBox: BoundingBox?

    val viewportIsAnimating: Boolean

    fun animator(): ValueAnimator?
}
