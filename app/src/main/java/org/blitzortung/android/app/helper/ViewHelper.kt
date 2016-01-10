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

package org.blitzortung.android.app.helper

import android.view.View

object ViewHelper {

    fun pxFromSp(view: View, sp: Float): Float {
        val displayMetrics = view.context.resources.displayMetrics
        return sp * displayMetrics.scaledDensity
    }

    fun pxFromDp(view: View, dp: Float): Float {
        val displayMetrics = view.context.resources.displayMetrics
        return dp * displayMetrics.density
    }
}
