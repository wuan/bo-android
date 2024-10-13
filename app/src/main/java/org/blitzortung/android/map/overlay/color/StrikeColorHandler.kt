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

package org.blitzortung.android.map.overlay.color

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class StrikeColorHandler @Inject constructor(preferences: SharedPreferences) : ColorHandler(preferences) {

    private val streetmapColors = mutableMapOf<IntArray, IntArray>()

    override fun getColors(target: ColorTarget): IntArray {
        val strikeColors = colorScheme.strikeColors
        return when (target) {
            ColorTarget.SATELLITE -> strikeColors
            ColorTarget.STREETMAP -> getCachedStreetmapStrikeColors(strikeColors)
        }
    }

    private fun getCachedStreetmapStrikeColors(strikeColors: IntArray): IntArray {
        if (!streetmapColors.containsKey(strikeColors)) {
            streetmapColors[strikeColors] = modifyBrightness(strikeColors, 0.8f)
        }
        return streetmapColors[strikeColors]!!
    }

    override fun getTextColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xff000000.toInt()
            ColorTarget.STREETMAP -> 0xffffffff.toInt()
        }
    }
}
