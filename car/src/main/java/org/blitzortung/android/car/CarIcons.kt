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

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

/**
 * Builds a [CarIcon] from a drawable resource.
 *
 * The drawable is rasterised before it is handed to the car host. Resource-based
 * icons are resolved by the host from the app's resource table, and the host may
 * keep using a cached bitmap when an app update shifts resource ids, showing the
 * wrong icon. A bitmap keeps the icon tied to the code that created it.
 */
internal fun carIcon(
    context: CarContext,
    resourceId: Int,
): CarIcon {
    val drawable = checkNotNull(context.getDrawable(resourceId))
    val width =
        drawable.intrinsicWidth.takeIf { it > 0 }
            ?: (DEFAULT_ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: width
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(Canvas(bitmap))
    return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
}

private const val DEFAULT_ICON_SIZE_DP = 24
