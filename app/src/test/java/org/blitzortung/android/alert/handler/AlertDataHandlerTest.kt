package org.blitzortung.android.alert.handler

import android.location.Location
import org.assertj.core.api.KotlinAssertions.assertThat
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.data.beans.DefaultStrike
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
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
        assertThat(sector).isEqualTo(sectorWithStrike(result))

        assertSectorAndRange(result, "foo", 2.5f, thresholdTime)
    }

    @Test
    fun testCheckWithinThresholdTimeFirstSectorAndRange1() {
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(2500f)
        `when`(location.bearingTo(any(Location::class.java))).thenReturn(90f)

        val result = alertDataHandler.checkStrikes(arrayListOf(strike), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull()
        assertThat(sector).isEqualTo(sectorWithStrike(result))

        assertSectorAndRange(result, "S", 2.5f, thresholdTime)
    }

    @Test
    fun testCheckWithinThresholdTimeSecondSectorAndRange1() {
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(2500f)
        `when`(location.bearingTo(any(Location::class.java))).thenReturn(180f)

        val result = alertDataHandler.checkStrikes(arrayListOf(strike), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull()
        assertThat(sector).isEqualTo(sectorWithStrike(result))

        assertSectorAndRange(result, "S", 2.5f, thresholdTime)
    }

    @Test
    fun testCheckWithinThresholdTimeAndOutOfAllRanges() {
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(5000.1f)

        val result = alertDataHandler.checkStrikes(arrayListOf(strike), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
        assertThat(rangeWithStrike(result)).isNull()
    }

    @Test
    fun testCheckOutOfThresholdTimeAndWithinRange2() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(2500.1f)
        `when`(location.bearingTo(any(Location::class.java))).thenReturn(-90f)

        val result = alertDataHandler.checkStrikes(listOf(strike), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
        assertSectorAndRange(result, "N", 5f, beforeThresholdTime)
    }

    @Test
    fun testCheckOutOfThresholdTimeAndAllRanges() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        `when`(location.distanceTo(any(Location::class.java))).thenReturn(5000.1f)

        val result = alertDataHandler.checkStrikes(listOf(strike), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
        assertThat(rangeWithStrike(result)).isNull()
    }

    private fun assertSectorAndRange(result: AlertResult, expectedSectorLabel: String, expectedRange: Float, expectedTime: Long) {
        val sectorWithStrike = sectorWithStrike(result)
        assertThat(sectorWithStrike).isNotNull()
        if (sectorWithStrike != null) {
            assertThat(sectorWithStrike.label).isEqualTo(expectedSectorLabel)
            val rangeWithStrike = sectorWithStrike.ranges.filter { it.strikeCount > 0 }.firstOrNull()
            assertThat(rangeWithStrike).isNotNull()
            if (rangeWithStrike != null) {
                assertThat(rangeWithStrike.strikeCount).isEqualTo(1)
                assertThat(rangeWithStrike.rangeMaximum).isEqualTo(expectedRange)
                assertThat(rangeWithStrike.latestStrikeTimestamp).isEqualTo(expectedTime)
            }
        }
    }

    private fun sectorWithStrike(result: AlertResult) = result.sectors.filter { !it.ranges.filter { it.strikeCount > 0 }.isEmpty() }.firstOrNull()

    private fun rangeWithStrike(result: AlertResult) = result.sectors.flatMap { it.ranges }.filter { it.strikeCount > 0 }.firstOrNull()
}
