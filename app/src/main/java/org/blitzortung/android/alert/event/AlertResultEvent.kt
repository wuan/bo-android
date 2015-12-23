package org.blitzortung.android.alert.event

import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.alert.data.AlertContext

data class AlertResultEvent(val alertContext: AlertContext, val alertResult: AlertResult?) : AlertEvent
