package org.blitzortung.android.alert.handler

import android.location.Location
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.data.beans.DefaultStrike
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.`any`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlertDataHandlerTest {

    private lateinit var strike: DefaultStrike

    @Mock
    private lateinit var location: Location

    private lateinit var strikeLocation: Location

    private var now: Long = 0

    private var thresholdTime: Long = 0

    private var beforeThresholdTime: Long = 0

    private lateinit var alertDataHandler: AlertDataHandler

    private lateinit var parameters: AlertParameters

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        strike = DefaultStrike(timestamp = now, longitude = 12.0f, latitude = 41.0f, altitude = 0, amplitude = 20.0f)

        strikeLocation = Location("")
        strikeLocation.longitude = strike.longitude.toDouble()
        strikeLocation.latitude = strike.latitude.toDouble()

        parameters = AlertParameters(
                alarmInterval = 10 * 60 * 1000,
                rangeSteps = arrayOf(2.5f, 5f),
                sectorLabels = arrayOf("S", "N"),
                measurementSystem = MeasurementSystem.METRIC
        )

        now = System.currentTimeMillis()
        thresholdTime = now - parameters.alarmInterval
        beforeThresholdTime = thresholdTime - 1

        alertDataHandler = AlertDataHandler()
    }

    @Test
    fun testCheckWithinThresholdTimeSingleSectorAndRange1() {
        parameters = parameters.copy(sectorLabels = arrayOf("foo"))
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(2500f)

        val result = alertDataHandler.checkStrikes(arrayListOf(strike), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull()

        assertThat(sector?.label).isEqualTo("foo")
    }

    @Test
    fun testCheckWithinThresholdTimeFirstSectorAndRange1() {
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(2500f)
        `when`(location.bearingTo(any(Location::class.java))).thenReturn(0f)

        val result = alertDataHandler.checkStrikes(arrayListOf(strike), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull()

        assertThat(sector?.label).isEqualTo("N")
    }

    @Test
    fun testCheckWithinThresholdTimeSecondSectorAndRange1() {
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(2500f)
        `when`(location.bearingTo(any(Location::class.java))).thenReturn(180f)

        val result = alertDataHandler.checkStrikes(arrayListOf(strike), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull()

        assertThat(sector?.label).isEqualTo("S")

        val rangeWithStrikes = getRangesWithStrikes(result).firstOrNull()
        assertThat(rangeWithStrikes).isNotNull()
        if (rangeWithStrikes != null) {
            assertThat(rangeWithStrikes.strikeCount).isEqualTo(1)
            assertThat(rangeWithStrikes.latestStrikeTimestamp).isEqualTo(thresholdTime)
        }
    }

    @Test
    fun testCheckWithinThresholdTimeAndOutOfAllRanges() {
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(5000.1f)

        val result = alertDataHandler.checkStrikes(arrayListOf(strike), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
        assertThat(getRangesWithStrikes(result).isEmpty()).isTrue()
    }

    @Test
    fun testCheckOutOfThresholdTimeAndWithinRange2() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(2500.1f)

        val result = alertDataHandler.checkStrikes(listOf(strike), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()

        val rangeWithStrikes = getRangesWithStrikes(result).firstOrNull()
        assertThat(rangeWithStrikes).isNotNull()
        if (rangeWithStrikes != null) {
            assertThat(rangeWithStrikes.strikeCount).isEqualTo(1)
            assertThat(rangeWithStrikes.latestStrikeTimestamp).isEqualTo(beforeThresholdTime)
        }
    }

    private fun getRangesWithStrikes(result: AlertResult) = result.sectors.flatMap { it.ranges }.filter { it.strikeCount > 0 }

    @Test
    fun testCheckOutOfThresholdTimeAndAllRanges() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(5000.1f)

        val result = alertDataHandler.checkStrikes(listOf(strike), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
    }
}
