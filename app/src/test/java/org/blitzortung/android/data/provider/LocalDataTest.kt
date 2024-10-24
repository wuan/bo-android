package org.blitzortung.android.data.provider

import android.location.Location
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

        assertThat(result.localReference?.x).isEqualTo(1)
        assertThat(result.localReference?.y).isEqualTo(2)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesLeftLowerCornerNegativeCoordinates() {
        val result = uut.updateParameters(parameters, createLocation(-7.5, -12.5))

        assertThat(result.localReference?.x).isEqualTo(-2)
        assertThat(result.localReference?.y).isEqualTo(-3)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesCenter() {
        val result = uut.updateParameters(parameters, createLocation(7.5, 12.5))

        assertThat(result.localReference?.x).isEqualTo(1)
        assertThat(result.localReference?.y).isEqualTo(2)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesUpperRightCornerInnerLimit() {
        val result = uut.updateParameters(parameters, createLocation(9.999, 14.999))

        assertThat(result.localReference?.x).isEqualTo(1)
        assertThat(result.localReference?.y).isEqualTo(2)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun calculatesUpperRightCorner() {
        val result = uut.updateParameters(parameters, createLocation(10.0, 15.0))

        assertThat(result.localReference?.x).isEqualTo(2)
        assertThat(result.localReference?.y).isEqualTo(3)
        assertThat(result.region).isEqualTo(LOCAL_REGION)
    }

    @Test
    fun fallsBackToGlobalWithoutLocation() {
        val result = uut.updateParameters(parameters, null)

        assertThat(result.localReference).isNull()
        assertThat(result.region).isEqualTo(GLOBAL_REGION)
    }

    fun createLocation(x: Double, y: Double): Location {
        return Location("").apply {
            longitude = x
            latitude = y
        }
    }
}