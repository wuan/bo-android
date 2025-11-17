/*

   Copyright 2019 Andreas Würl

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

import org.blitzortung.android.app.R

enum class MeasurementSystem(val unitNameString: Int, private val factor: Float) {
    METRIC(R.string.unit_km, 1000.0f),
    IMPERIAL(R.string.unit_miles, 1609.344f),
    ;

    fun calculateDistance(meters: Float): Float {
        return meters / factor
    }
}
