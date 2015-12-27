package org.blitzortung.android.util

import android.content.Context

object UI {
    fun isTablet(context: Context): Boolean {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    fun padding(context: Context) :Float {
        return if (UI.isTablet(context)) 8f else 5f
    }

    fun textSize(context: Context) :Float {
        return if (UI.isTablet(context)) 18f else 12f
    }

    fun sizeFactor(context: Context) :Float {
        return if (UI.isTablet(context)) 1.8f else 1f
    }
 }