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

open class StrikeColorHandler(preferences: SharedPreferences) : ColorHandler(preferences) {

    override fun getColors(target: ColorTarget): IntArray {
        val strikeColors = colorScheme.strikeColors
        return when (target) {
            ColorTarget.SATELLITE -> strikeColors
            ColorTarget.STREETMAP -> modifyBrightness(strikeColors, 0.8f)
        }
    }

    override fun getTextColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xff000000.toInt()
            ColorTarget.STREETMAP -> 0xffffffff.toInt()
        }
    }
}
