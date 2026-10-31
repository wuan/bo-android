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

import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer

/**
 * Bridges the host's [SurfaceCallback] to a [CarMapRenderer].
 */
class CarMapSurfaceCallback(
    private val renderer: CarMapRenderer,
) : SurfaceCallback {
    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) = renderer.onSurfaceAvailable(surfaceContainer)

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) =
        renderer.onSurfaceDestroyed(surfaceContainer)

    override fun onScroll(
        distanceX: Float,
        distanceY: Float,
    ) = renderer.onScroll(distanceX, distanceY)

    override fun onFling(
        velocityX: Float,
        velocityY: Float,
    ) = renderer.onFling(velocityX, velocityY)

    override fun onScale(
        focusX: Float,
        focusY: Float,
        scaleFactor: Float,
    ) = renderer.onScale(focusX, focusY, scaleFactor)

    override fun onClick(
        x: Float,
        y: Float,
    ) = renderer.onClick(x, y)
}
