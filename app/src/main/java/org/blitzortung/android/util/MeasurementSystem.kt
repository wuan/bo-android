package org.blitzortung.android.util

enum class MeasurementSystem private constructor(val unitName: String, private val factor: Float) {
    METRIC("km", 1000.0f),
    IMPERIAL("mi.", 1609.344f);

    fun calculateDistance(meters: Float): Float {
        return meters / factor
    }
}
