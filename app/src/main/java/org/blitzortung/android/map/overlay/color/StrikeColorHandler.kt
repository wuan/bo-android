package org.blitzortung.android.map.overlay.color

import android.content.SharedPreferences

open class StrikeColorHandler(preferences: SharedPreferences) : ColorHandler(preferences) {

    public override fun getColors(target: ColorTarget): IntArray {
        return when (target) {
            ColorTarget.SATELLITE -> colorScheme!!.strikeColors
            ColorTarget.STREETMAP -> modifyBrightness(colorScheme!!.strikeColors, 0.8f)
        }
    }

    override fun getTextColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> -16777216
            ColorTarget.STREETMAP -> -1
        }
    }
}
