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

package org.blitzortung.android.map

import android.animation.ValueAnimator
import java.lang.reflect.Field
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView

/**
 * Shared accessor for the osmdroid camera animator that is currently running.
 *
 * osmdroid keeps the animator in the private [MapController.mCurrentAnimator]
 * field and does not clear it when the animation ends, so [current] first checks
 * the public animation state. When the field is unavailable (for example after
 * an osmdroid upgrade) it returns `null` instead of failing, letting callers
 * fall back to applying the camera immediately.
 */
object MapCameraAnimator {
    private val animatorField: Field? =
        runCatching {
            MapController::class.java
                .getDeclaredField("mCurrentAnimator")
                .apply { isAccessible = true }
        }.getOrNull()

    /** The live camera animator of [mapView], or `null` while the map is idle. */
    fun current(mapView: MapView): ValueAnimator? {
        if (!mapView.isAnimating) {
            return null
        }
        val field = animatorField ?: return null
        return runCatching { field.get(mapView.controller) as? ValueAnimator }.getOrNull()
    }
}
