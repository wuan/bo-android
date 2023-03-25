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

package org.blitzortung.android.data

import org.blitzortung.android.data.provider.GLOBAL_REGION
import org.blitzortung.android.data.provider.LOCAL_REGION

data class Parameters(
    val region: Int = -1,
    val rasterBaselength: Int = 0,
    override val intervalDuration: Int = 0,
    override val intervalOffset: Int = 0,
    val countThreshold: Int = 0,
    val localReference: LocalReference? = null
) : TimeIntervalWithOffset {

    val isGlobal: Boolean = region == GLOBAL_REGION

    val isLocal: Boolean = region == LOCAL_REGION

    fun isRealtime(): Boolean = intervalOffset == 0

}

data class LocalReference(
    val x: Int,
    val y: Int
)