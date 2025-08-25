package org.blitzortung.android.data.provider

import android.location.Location
import android.util.Log
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.data.DataArea
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.osmdroid.util.BoundingBox
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

private const val MIN_DATA_SCALE = 5

private const val MAX_DATA_SCALE = 20

private const val LOCAL_DATA_SCALE = MIN_DATA_SCALE

private const val DATA_AREA_SIZE_FACTOR = 0.5

const val LOCAL_REGION_GRID_SIZE_THRESHOLD = 25000

@Singleton
class LocalData @Inject constructor() {

    var dataArea: DataArea? = null

    private var gridParameters: GridParameters? = null

    fun updateParameters(parameters: Parameters, location: Location?): Parameters {
        return when (parameters.region) {
            LOCAL_REGION -> {
                if (location != null) {
                    Log.d(LOG_TAG, "LocalData.updateParameters() local")
                    val x = calculateLocalCoordinate(location.longitude)
                    val y = calculateLocalCoordinate(location.latitude)
                    parameters.copy(dataArea = DataArea(x, y, LOCAL_DATA_SCALE))
                } else {
                    Log.d(LOG_TAG, "LocalData.updateParameters() local -> global")
                    globalParameters(parameters)
                }
            }

            GLOBAL_REGION -> {
                if (dataArea != null && parameters.gridSize < LOCAL_REGION_GRID_SIZE_THRESHOLD) {
                    Log.d(LOG_TAG, "LocalData.updateParameters() global -> local ($dataArea)")
                    parameters.copy(region = LOCAL_REGION, dataArea = dataArea)
                } else {
                    Log.d(LOG_TAG, "LocalData.updateParameters() global")
                    globalParameters(parameters)
                }
            }

            else -> {
                parameters
            }
        }
    }

    private fun globalParameters(parameters: Parameters): Parameters = parameters.copy(
        region = GLOBAL_REGION,
        dataArea = null,
        gridSize = max(parameters.gridSize, LOCAL_REGION_GRID_SIZE_THRESHOLD)
    )

    fun update(boundingBox: BoundingBox, force: Boolean = false): Boolean {
        val dataArea = calculateDataArea(boundingBox)

        val gridParameters = gridParameters
        val isOutside = if (gridParameters == null) false else this@LocalData.isOutside(boundingBox, gridParameters)
        val isChanged = this.dataArea != dataArea
        return if (
            gridParameters != null && isOutside && isChanged ||
            gridParameters == null && isChanged ||
            force
        ) {
            if (dataArea != null) {
                Log.d(
                    LOG_TAG,
                    "LocalData.update() $dataArea from center ${round(boundingBox.centerLongitude * 100) / 100}, ${
                        round(boundingBox.centerLatitude * 100) / 100
                    } -> ${dataArea.x1}..${dataArea.x2} ${dataArea.y1}..${dataArea.y2}, span: ${
                        round(boundingBox.longitudeSpanWithDateLine * 100) / 100
                    }, ${round(boundingBox.latitudeSpan * 100) / 100} "
                )
            } else {
                Log.d( LOG_TAG, "LocalData.update() disabled from center ${round(boundingBox.centerLongitude * 100) / 100}, ${ round(boundingBox.centerLatitude * 100) / 100 } " )
            }
            this.dataArea = dataArea
            true
        } else {
            false
        }
    }

    private fun calculateDataArea(boundingBox: BoundingBox): DataArea? {
        val scale = calculateDataAreaScale(boundingBox)
        return if (scale != null) {
            val x = calculateLocalCoordinate(boundingBox.centerLongitude, scale)
            val y = calculateLocalCoordinate(boundingBox.centerLatitude, scale)
            DataArea(x, y, scale)
        } else {
            null
        }
    }

    private fun calculateDataAreaScale(boundingBox: BoundingBox): Int? {
        val maxExtent = max(boundingBox.longitudeSpanWithDateLine, boundingBox.latitudeSpan)
        val targetValue = DATA_AREA_SIZE_FACTOR * maxExtent
        val dataArea = (ceil(targetValue / MIN_DATA_SCALE) * MIN_DATA_SCALE).toInt()
        return if (dataArea <= MAX_DATA_SCALE) dataArea else null
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

fun calculateLocalCoordinate(coordinate: Double, scale: Int = LOCAL_DATA_SCALE): Int {
    return (coordinate / scale).toInt() - if (coordinate < 0) 1 else 0
}

internal const val LOCAL_REGION = -1
internal const val GLOBAL_REGION = 0
