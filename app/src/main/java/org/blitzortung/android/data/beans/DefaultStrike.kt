package org.blitzortung.android.data.beans

import java.io.Serializable

data class DefaultStrike(
        override val timestamp: Long = 0,
        override val longitude: Float = 0f,
        override val latitude: Float = 0f,
        val altitude: Int = 0,
        val amplitude: Float = 0f,
        val stationCount: Short = 0,
        val lateralError: Float = 0f) : Strike, Serializable {

    override val multiplicity = 1

    companion object {
        private val serialVersionUID = 4201042078597105622L
    }
}
