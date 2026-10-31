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

package org.blitzortung.android.util

import android.content.Context

/**
 * Screen-size dependent scaling factors, shared by the map overlays and the
 * phone views so both use the same values.
 */
object ScreenScaling {
    fun isTablet(context: Context): Boolean = context.resources.configuration.smallestScreenWidthDp >= 600

    fun padding(context: Context): Float = padding(isTablet(context))

    fun padding(scaleForTablet: Boolean): Float = if (scaleForTablet) 8f else 5f

    fun textSize(context: Context): Float = textSize(isTablet(context))

    fun textSize(scaleForTablet: Boolean): Float = 14f * textSizeFactor(scaleForTablet)

    fun sizeFactor(context: Context): Float = sizeFactor(isTablet(context))

    fun sizeFactor(scaleForTablet: Boolean): Float = if (scaleForTablet) 1.8f else 1f

    fun textSizeFactor(context: Context): Float = textSizeFactor(isTablet(context))

    fun textSizeFactor(scaleForTablet: Boolean): Float = if (scaleForTablet) 1.4f else 1f
}
