package org.blitzortung.android.alert.data

import android.location.Location
import org.blitzortung.android.alert.AlertParameters

data class AlertContext(
        val location: Location,
        val alertParameters: AlertParameters,
        val sectors: List<AlertSector>
) {
}
