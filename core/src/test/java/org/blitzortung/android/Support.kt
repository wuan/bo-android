package org.blitzortung.android

import android.location.Location
import org.blitzortung.android.location.LocationUpdate

fun createLocation(
    x: Double,
    y: Double,
): Location =
    Location("").apply {
        longitude = x
        latitude = y
    }

fun createLocationEvent(
    x: Double,
    y: Double,
) = LocationUpdate(createLocation(x, y))
