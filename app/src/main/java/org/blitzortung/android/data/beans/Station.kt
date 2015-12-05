package org.blitzortung.android.data.beans

class Station(override val longitude: Float, override val latitude: Float, val name: String, val offlineSince: Long) : Location {

    val state: State
        get() {
            if (offlineSince == OFFLINE_SINCE_NOT_SET) {
                return State.ON
            } else {
                val now = System.currentTimeMillis()

                val minutesAgo = (now - offlineSince) / 1000 / 60

                if (minutesAgo > 24 * 60) {
                    return State.OFF
                } else if (minutesAgo > 15) {
                    return State.DELAYED
                } else {
                    return State.ON
                }
            }
        }

    enum class State {
        ON, DELAYED, OFF
    }

    companion object {
        val OFFLINE_SINCE_NOT_SET: Long = -1
    }
}
