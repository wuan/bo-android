package org.blitzortung.android.app.permission

import android.location.LocationManager

enum class LocationProviderRelation(val providerName: String) {
    GPS(LocationManager.GPS_PROVIDER),
    PASSIVE(LocationManager.PASSIVE_PROVIDER),
    NETWORK(LocationManager.NETWORK_PROVIDER),
    ;

    companion object {
        val byProviderName: Map<String, LocationProviderRelation> =
            entries.groupBy { it.providerName }.mapValues { it.value.first() }
        val byOrdinal: Map<Int, LocationProviderRelation> =
            entries.groupBy { it.ordinal }.mapValues { it.value.first() }
    }
}
