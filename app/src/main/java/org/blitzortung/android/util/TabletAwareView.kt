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
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper.pxFromDp
import org.blitzortung.android.app.helper.ViewHelper.pxFromSp

open class TabletAwareView(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int,
) : View(context, attrs, defStyle) {
    protected var padding: Float = 0.0f
    protected var textSize: Float = 0.0f
    protected var sizeFactor: Float = 0.0f

    init {
        context.withStyledAttributes(attrs, R.styleable.View, defStyle, 0) {

            val scaleForTablet = getBoolean(R.styleable.View_tablet_scaleable, false) && isTablet(context)

            padding = pxFromDp(context, padding(scaleForTablet))
            textSize = pxFromSp(context, textSize(scaleForTablet))
            sizeFactor = sizeFactor(scaleForTablet)
        }
    }

    @SuppressWarnings("unused")
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    @SuppressWarnings("unused")
    constructor(context: Context) : this(context, null, 0)

    companion object {
        fun isTablet(context: Context): Boolean {
            return if (isAtLeast(Build.VERSION_CODES.HONEYCOMB_MR2)) {
                context.resources.configuration.smallestScreenWidthDp >= 600
            } else {
                false
            }
        }

        fun padding(context: Context): Float {
            return padding(isTablet(context))
        }

        fun padding(scaleForTablet: Boolean): Float {
            return if (scaleForTablet) 8f else 5f
        }

        fun textSize(context: Context): Float {
            return textSize(isTablet(context))
        }

        fun textSize(scaleForTablet: Boolean): Float {
            return 14f * textSizeFactor(scaleForTablet)
        }

        fun sizeFactor(context: Context): Float {
            return sizeFactor(isTablet(context))
        }

        fun sizeFactor(scaleForTablet: Boolean): Float {
            return if (scaleForTablet) 1.8f else 1f
        }

        fun textSizeFactor(context: Context): Float {
            return textSizeFactor(isTablet(context))
        }

        fun textSizeFactor(scaleForTablet: Boolean): Float {
            return if (scaleForTablet) 1.4f else 1f
        }
    }
}
