package org.blitzortung.android.map.overlay.color

import android.content.SharedPreferences

class ParticipantColorHandler(preferences: SharedPreferences) : ColorHandler(preferences) {

    private val satelliteViewColors = intArrayOf(0xff88ff22.toInt(), 0xffff9900.toInt(), 0xffff0000.toInt())

    private val mapColors = intArrayOf(0xff448811.toInt(), 0xff884400.toInt(), 0xff880000.toInt())

    public override fun getColors(target: ColorTarget): IntArray {
        return when (target) {
            ColorTarget.SATELLITE -> satelliteViewColors
            ColorTarget.STREETMAP -> mapColors
        }
    }
}
