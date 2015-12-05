package org.blitzortung.android.data.beans

import java.io.Serializable

data class RasterElement(
        override val timestamp: Long,
        override val longitude: Float,
        override val latitude: Float,
        override val multiplicity: Int) : Strike, Serializable {

    companion object {
        private val serialVersionUID = 6765788323616893614L
    }
}
