package org.blitzortung.android.dialogs

import android.content.SharedPreferences

import org.blitzortung.android.map.overlay.color.ColorTarget
import org.blitzortung.android.map.overlay.color.StrikeColorHandler

class AlertDialogColorHandler(preferences: SharedPreferences) : StrikeColorHandler(preferences) {

    override fun getTextColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xFF000000
            ColorTarget.STREETMAP -> 0xFFFFFFFF
            else -> 0xFF000000
        }.toInt()
    }

    override fun getLineColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xff555555
            ColorTarget.STREETMAP -> 0xff888888
            else -> 0xff555555
        }.toInt()
    }

    override fun getBackgroundColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xff888888
            ColorTarget.STREETMAP -> 0xff555555
            else -> 0xff888888
        }.toInt()
    }
}