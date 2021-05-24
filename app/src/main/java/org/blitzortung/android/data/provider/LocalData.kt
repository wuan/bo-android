package org.blitzortung.android.data.provider

import android.location.Location
import org.blitzortung.android.data.LocalReference
import org.blitzortung.android.data.Parameters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalData @Inject constructor() {
    fun updateParameters(parameters: Parameters, location: Location?): Parameters {
        return if (parameters.region == LOCAL_REGION) {
            if (location != null) {
                val x = calculateLocalRegion(location.longitude)
                val y = calculateLocalRegion(location.latitude)
                parameters.copy(localReference = LocalReference(x, y))
            } else {
                parameters.copy(region = GLOBAL_REGION)
            }
        } else {
            parameters
        }
    }

    private fun calculateLocalRegion(value: Double): Int {
        return (value / 5).toInt() - if (value < 0) 1 else 0
    }
}

internal const val LOCAL_REGION = -1
internal const val GLOBAL_REGION = 0
