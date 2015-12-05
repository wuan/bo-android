package org.blitzortung.android.map.overlay.color

import android.content.SharedPreferences
import android.graphics.Color

import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.data.TimeIntervalWithOffset

abstract class ColorHandler(private val preferences: SharedPreferences) {

    var colorScheme: ColorScheme? = null
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
        section = limitIndexToValidRange(section)

        return section
    }

    fun getColor(now: Long, eventTime: Long, intervalDuration: Int): Int {
        return getColor(getColorSection(now, eventTime, intervalDuration, 0))
    }

    fun getColor(section: Int): Int {
        var section_ = section
        section_ = limitIndexToValidRange(section_)
        return colors[section_]
    }

    private fun limitIndexToValidRange(section_: Int): Int {
        var section_ = section_
        section_ = Math.min(section_, colors.size - 1)
        section_ = Math.max(section_, 0)
        return section_
    }

    val textColor: Int
        get() = getTextColor(target)

    open fun getTextColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> -1
            ColorTarget.STREETMAP -> -16777216
        }
    }

    val lineColor: Int
        get() = getLineColor(target)

    open fun getLineColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> -1
            ColorTarget.STREETMAP -> -16777216
        }
    }

    val backgroundColor: Int
        get() = getBackgroundColor(target)

    open fun getBackgroundColor(target: ColorTarget): Int {
        return when (target) {
            ColorTarget.SATELLITE -> 0
            ColorTarget.STREETMAP -> 16777215
        }
    }

    val numberOfColors: Int
        get() = colors.size

    fun modifyBrightness(colors: IntArray, factor: Float): IntArray {
        val result = IntArray(colors.size)

        val HSVValues = FloatArray(3)

        for (index in colors.indices) {
            Color.colorToHSV(colors[index], HSVValues)
            HSVValues[2] *= factor
            result[index] = Color.HSVToColor(HSVValues)
        }

        return result
    }

}