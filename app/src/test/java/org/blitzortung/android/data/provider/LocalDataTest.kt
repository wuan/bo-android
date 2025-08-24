package org.blitzortung.android.data.provider

import android.location.Location
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Reference
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.GridParameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.BoundingBox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalDataTest {

    private lateinit var uut: LocalData

    private lateinit var parameters: Parameters

    @Before
    fun setUp() {
        uut = LocalData()
        parameters = Parameters(region = LOCAL_REGION, gridSize = 10000)
    }

    @Test
    fun calculatesLeftLowerCorner() {
        val result = uut.updateParameters(parameters, createLocation(5.0, 10.0))

        assertThat(result.reference?.x).isEqualTo(1)
        assertThat(result.reference?.y).isEqualTo(2)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesLeftLowerCornerNegativeCoordinates() {
        val result = uut.updateParameters(parameters, createLocation(-7.5, -12.5))

        assertThat(result.reference?.x).isEqualTo(-2)
        assertThat(result.reference?.y).isEqualTo(-3)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesCenter() {
        val result = uut.updateParameters(parameters, createLocation(7.5, 12.5))

        assertThat(result.reference?.x).isEqualTo(1)
        assertThat(result.reference?.y).isEqualTo(2)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesUpperRightCornerInnerLimit() {
        val result = uut.updateParameters(parameters, createLocation(9.999, 14.999))

        assertThat(result.reference?.x).isEqualTo(1)
        assertThat(result.reference?.y).isEqualTo(2)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesUpperRightCorner() {
        val result = uut.updateParameters(parameters, createLocation(10.0, 15.0))

        assertThat(result.reference?.x).isEqualTo(2)
        assertThat(result.reference?.y).isEqualTo(3)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun fallsBackToGlobalWithoutLocation() {
        val result = uut.updateParameters(parameters, null)

        assertThat(result.reference).isNull()
        assertThat(result.region).isEqualTo(GLOBAL_REGION)
    }

    @Test
    fun updateLocalData() {
        val boundingBox = BoundingBox(46.0, 11.0, 45.0, 10.0)
        val result = uut.update(boundingBox)

        assertThat(result).isTrue
        assertThat(uut.reference).isEqualTo(Reference(2, 9, 5))
    }

    @Test
    fun noUpdateLocalDataOnGridInside() {
        val boundingBox = BoundingBox(46.0, 11.0, 45.0, 10.0)
        uut.update(boundingBox)

        uut.storeResult(GridParameters(10.0, 46.0, 0.2, 0.2, 5, 5))
        val result = uut.update(boundingBox)

        assertThat(result).isFalse
        assertThat(uut.reference).isEqualTo(Reference(2, 9, 5))
    }

    @Test
    fun noUpdateLocalDataOnGridOutsideWithoutUpdatedParameters() {
        val boundingBox = BoundingBox(46.0, 11.0, 45.0, 10.0)
        uut.update(boundingBox)

        uut.storeResult(GridParameters(10.0, 46.0, 0.2, 0.2, 5, 3))
        val result = uut.update(boundingBox)

        assertThat(result).isFalse
        assertThat(uut.reference).isEqualTo(Reference(2, 9, 5))
    }

    @Test
    fun updateLocalDataOnGridOutsideWithUpdatedParameters() {
        val boundingBox1 = BoundingBox(46.0, 11.0, 45.0, 10.0)
        uut.update(boundingBox1)

        uut.storeResult(GridParameters(10.0, 46.0, 0.2, 0.2, 5, 5))
        val boundingBox2 = BoundingBox(55.0, 11.0, 45.0, 10.0)
        val result = uut.update(boundingBox2)

        assertThat(result).isTrue
        assertThat(uut.reference).isEqualTo(Reference(2, 10,5))
    }

    @Test
    fun noUpdateOfLocalDataForSmallMovement() {
        val boundingBox1 = BoundingBox(46.0, 11.0, 45.0, 10.0)
        uut.update(boundingBox1)

        val boundingBox2 = BoundingBox(50.0, 15.0, 49.0, 14.0)
        val result = uut.update(boundingBox2)

        assertThat(result).isFalse
        assertThat(uut.reference).isEqualTo(Reference(2, 9, 5))
    }

    @Test
    fun forcedUpdateOfLocalData() {
        val boundingBox1 = BoundingBox(46.0, 11.0, 45.0, 10.0)
        uut.update(boundingBox1)

        val boundingBox2 = BoundingBox(50.0, 15.0, 49.0, 14.0)
        val result = uut.update(boundingBox2, force = true)

        assertThat(result).isTrue
        assertThat(uut.reference).isEqualTo(Reference(2, 9, 5))
    }

    @Test
    fun updateOfLocalDataForMovementExceedingTheDataArea() {
        val boundingBox1 = BoundingBox(46.0, 11.0, 45.0, 10.0)
        uut.update(boundingBox1)

        val boundingBox2 = BoundingBox(51.0, 11.0, 50.0, 10.0)
        val result = uut.update(boundingBox2)

        assertThat(result).isTrue
        assertThat(uut.reference).isEqualTo(Reference(2, 10, 5))
    }

    @Test
    fun noUpdateOfLocalDataOnZoomWithinArea() {
        val boundingBox1 = BoundingBox(45.5, 10.5, 44.5, 9.5)
        uut.update(boundingBox1)

        val boundingBox2 = BoundingBox(50.0, 15.0, 40.0, 5.0)
        val result = uut.update(boundingBox2)

        assertThat(result).isFalse
        assertThat(uut.reference).isEqualTo(Reference(2, 9, 5))
    }

    @Test
    fun updateOfLocalDataOnZoomOutsideArea() {
        val boundingBox1 = BoundingBox(45.5, 10.5, 44.5, 9.5)
        uut.update(boundingBox1)

        val boundingBox2 = BoundingBox(51.0, 15.0, 40.0, 5.0)
        val result = uut.update(boundingBox2)

        assertThat(result).isTrue
        assertThat(uut.reference).isEqualTo(Reference(1, 4, 10))
    }

    @Test
    fun updateOfLocalDataOnZoomOutsideLocalArea() {
        val boundingBox1 = BoundingBox(45.5, 10.5, 44.5, 9.5)
        uut.update(boundingBox1)

        val boundingBox2 = BoundingBox(51.0, 15.0, -40.0, 5.0)
        val result = uut.update(boundingBox2)

        assertThat(result).isTrue
        assertThat(uut.reference).isNull()
    }

    fun createLocation(x: Double, y: Double): Location {
        return Location("").apply {
            longitude = x
            latitude = y
        }
    }
}
