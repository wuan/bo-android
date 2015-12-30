package org.blitzortung.android.alert.event

import org.blitzortung.android.alert.AlertResult

data class AlertResultEvent(
        val alertResult: AlertResult?
) : AlertEvent
