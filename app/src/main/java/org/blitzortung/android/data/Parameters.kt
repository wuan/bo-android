package org.blitzortung.android.data

data class Parameters(
        val region: Int = -1,
        val rasterBaselength: Int = 0,
        override val intervalDuration: Int = 0,
        override val intervalOffset: Int = 0,
        val countThreshold: Int = 0) : TimeIntervalWithOffset {

    fun isRealtime(): Boolean = intervalOffset == 0

    fun isRaster(): Boolean = rasterBaselength != 0
}
