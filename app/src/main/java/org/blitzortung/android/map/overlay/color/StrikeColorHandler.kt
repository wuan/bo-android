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
