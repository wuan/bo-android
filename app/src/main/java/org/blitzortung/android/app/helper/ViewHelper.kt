package org.blitzortung.android.app.helper

import android.util.DisplayMetrics
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
