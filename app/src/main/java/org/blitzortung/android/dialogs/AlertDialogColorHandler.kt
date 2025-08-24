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

package org.blitzortung.android.dialogs

import android.content.SharedPreferences

import org.blitzortung.android.map.overlay.color.ColorTarget
import org.blitzortung.android.map.overlay.color.StrikeColorHandler

class AlertDialogColorHandler(preferences: SharedPreferences) : StrikeColorHandler(preferences) {

    override fun getTextColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xFF000000
            ColorTarget.STREETMAP -> 0xFFFFFFFF
        }.toInt()
    }

    override fun getLineColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xff555555
            ColorTarget.STREETMAP -> 0xff888888
        }.toInt()
    }

    override fun getBackgroundColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xff888888
            ColorTarget.STREETMAP -> 0xff555555
        }.toInt()
    }
}
