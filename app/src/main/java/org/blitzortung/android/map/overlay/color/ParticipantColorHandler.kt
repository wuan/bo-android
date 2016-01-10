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

class ParticipantColorHandler(preferences: SharedPreferences) : ColorHandler(preferences) {

    private val satelliteViewColors = intArrayOf(0xff88ff22.toInt(), 0xffff9900.toInt(), 0xffff0000.toInt())

    private val mapColors = intArrayOf(0xff448811.toInt(), 0xff884400.toInt(), 0xff880000.toInt())

    override fun getColors(target: ColorTarget): IntArray {
        return when (target) {
            ColorTarget.SATELLITE -> satelliteViewColors
            ColorTarget.STREETMAP -> mapColors
        }
    }
}
