package org.blitzortung.android.data.provider

import android.location.Location
import android.util.Log
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.data.LocalReference
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.osmdroid.util.BoundingBox
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

private const val MIN_DATA_AREA = 5

private const val LOCAL_DATA_AREA = MIN_DATA_AREA

private const val DATA_AREA_SCALING = 0.5

private const val LOCAL_REGION_THRESHOLD = 10000

@Singleton
class LocalData @Inject constructor() {

    var localReference: LocalReference? = null
    var dataArea: Int = LOCAL_DATA_AREA
    private var gridParameters: GridParameters? = null

    fun updateParameters(parameters: Parameters, location: Location?): Parameters {
        return when (parameters.region) {
            LOCAL_REGION -> {
                if (location != null) {
                    Log.d(LOG_TAG, "LocalData.updateParameters() local")
                    val x = calculateLocalCoordinate(location.longitude)
                    val y = calculateLocalCoordinate(location.latitude)
                    parameters.copy(localReference = LocalReference(x, y), dataArea = LOCAL_DATA_AREA)
                } else {
                    Log.d(LOG_TAG, "LocalData.updateParameters() local -> global")
                    parameters.copy(region = GLOBAL_REGION, localReference = null, dataArea = LOCAL_DATA_AREA)
                }
            }

            GLOBAL_REGION -> {
                if (localReference != null && parameters.gridSize <= LOCAL_REGION_THRESHOLD) {
                    Log.d(LOG_TAG, "LocalData.updateParameters() global -> local")
                    parameters.copy(region = LOCAL_REGION, localReference = localReference, dataArea = dataArea)
                } else {
                    Log.d(LOG_TAG, "LocalData.updateParameters() global")
                    parameters.copy(region = GLOBAL_REGION, localReference = null, dataArea = LOCAL_DATA_AREA)
                }
            }

            else -> {
                parameters
            }
        }
    }

    fun update(boundingBox: BoundingBox, force: Boolean = false): Boolean {
        val dataArea = calculateDataArea(boundingBox)
        val xPos = calculateLocalCoordinate(boundingBox.centerLongitude, dataArea)
        val yPos = calculateLocalCoordinate(boundingBox.centerLatitude, dataArea)
        val localReference = LocalReference(xPos, yPos)

        val gridParameters = gridParameters
        val isOutside = if (gridParameters == null) false else this@LocalData.isOutside(boundingBox, gridParameters)
        val isChanged = this.dataArea != dataArea || this.localReference != localReference
        return if (
            gridParameters != null && isOutside && isChanged ||
            gridParameters == null && isChanged ||
            force
        ) {
            Log.d(
                LOG_TAG,
                "LocalData.update() $xPos, $yPos ($dataArea) from center ${round(boundingBox.centerLongitude * 100) / 100}, ${
                    round(boundingBox.centerLatitude * 100) / 100
                } -> ${xPos * dataArea}..${(xPos + 1) * dataArea} ${yPos * dataArea}..${(yPos + 1) * dataArea}, span: ${
                    round(boundingBox.longitudeSpanWithDateLine * 100) / 100
                }, ${round(boundingBox.latitudeSpan * 100) / 100} "
            )
            this.dataArea = dataArea
            this.localReference = localReference
            true
        } else {
            false
        }
    }

    private fun calculateDataArea(boundingBox: BoundingBox): Int {
        val maxExtent = max(boundingBox.longitudeSpanWithDateLine, boundingBox.latitudeSpan)
        val targetValue = DATA_AREA_SCALING * maxExtent
        return (ceil(targetValue / MIN_DATA_AREA) * MIN_DATA_AREA).toInt()
    }

    private fun isOutside(boundingBox: BoundingBox, gridParameters: GridParameters): Boolean {
        return boundingBox.lonWest < gridParameters.longitudeStart ||
                boundingBox.lonEast > gridParameters.longitudeEnd ||
                boundingBox.latNorth > gridParameters.latitudeStart ||
                boundingBox.latSouth < gridParameters.latitudeEnd
    }

    fun storeResult(gridParameters: GridParameters?) {
        this.gridParameters = gridParameters
    }
}

fun calculateLocalCoordinate(value: Double, dataArea: Int = LOCAL_DATA_AREA): Int {
    return (value / dataArea).toInt() - if (value < 0) 1 else 0
}

internal const val LOCAL_REGION = -1
internal const val GLOBAL_REGION = 0
