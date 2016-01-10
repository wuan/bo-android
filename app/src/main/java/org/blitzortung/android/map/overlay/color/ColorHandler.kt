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

package org.blitzortung.android.map.overlay.color

import android.content.SharedPreferences
import android.graphics.Color

import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.TimeIntervalWithOffset

abstract class ColorHandler(private val preferences: SharedPreferences) {

    var colorScheme: ColorScheme = ColorScheme.BLITZORTUNG
        private set

    private lateinit var target: ColorTarget

    init {
        updateTarget()
    }

    fun updateTarget() {
        target = ColorTarget.valueOf(preferences.getString(PreferenceKey.MAP_TYPE.toString(), "SATELLITE"))
        colorScheme = ColorScheme.valueOf(preferences.getString(PreferenceKey.COLOR_SCHEME.toString(), ColorScheme.BLITZORTUNG.toString()))
    }

    val colors: IntArray
        get() = getColors(target)

    protected abstract fun getColors(target: ColorTarget): IntArray

    fun getColorSection(now: Long, eventTime: Long, timeIntervalWithOffset: TimeIntervalWithOffset): Int {
        return getColorSection(now, eventTime, timeIntervalWithOffset.intervalDuration, timeIntervalWithOffset.intervalOffset)
    }

    private fun getColorSection(now: Long, eventTime: Long, intervalDuration: Int, intervalOffset: Int): Int {
        val minutesPerColor = intervalDuration / colors.size
        var section = (now + intervalOffset * 60 * 1000 - eventTime).toInt() / 1000 / 60 / minutesPerColor
        return limitToValidRange(section)
    }

    fun getColor(now: Long, eventTime: Long, intervalDuration: Int): Int {
        return getColor(getColorSection(now, eventTime, intervalDuration, 0))
    }

    fun getColor(index: Int): Int {
        return colors[limitToValidRange(index)]
    }

    private fun limitToValidRange(index: Int): Int {
        return Math.max(Math.min(index, colors.size - 1), 0)
    }

    val textColor: Int
        get() = getTextColor(target)

    open fun getTextColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xffffffff.toInt()
            ColorTarget.STREETMAP -> 0xff000000.toInt()
        }
    }

    val lineColor: Int
        get() = getLineColor(target)

    open fun getLineColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0xffffffff.toInt()
            ColorTarget.STREETMAP -> 0xff000000.toInt()
        }
    }

    val backgroundColor: Int
        get() = getBackgroundColor(target)

    open fun getBackgroundColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0x00000000.toInt()
            ColorTarget.STREETMAP -> 0x00ffffff.toInt()
        }
    }

    val numberOfColors: Int
        get() = colors.size

    fun modifyBrightness(colors: IntArray, factor: Float): IntArray {
        val HSVValues = FloatArray(3)

        return colors.map {
            Color.colorToHSV(it, HSVValues)
            HSVValues[2] *= factor
            Color.HSVToColor(HSVValues)
        }.toIntArray()
    }

}