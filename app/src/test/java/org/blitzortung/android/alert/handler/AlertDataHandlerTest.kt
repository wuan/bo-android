package org.blitzortung.android.alert.handler

import android.location.Location
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.AlertResult
import org.blitzortung.android.data.beans.DefaultStrike
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AlertDataHandlerTest {

    private lateinit var strike: DefaultStrike

    @MockK
    private lateinit var location: Location

    private lateinit var strikeLocation: Location

    private var now: Long = 0

    private var thresholdTime: Long = 0

    private var beforeThresholdTime: Long = 0

    private lateinit var alertDataHandler: AlertDataHandler

    private lateinit var parameters: AlertParameters

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        strike = DefaultStrike(timestamp = now, longitude = 12.0, latitude = 41.0, altitude = 0, amplitude = 20.0f)

        strikeLocation = Location("").apply {
            longitude = strike.longitude
            latitude = strike.latitude
        }

        parameters = AlertParameters(
                alarmInterval = 10 * 60 * 1000,
                rangeSteps = arrayOf(2.5f, 5f),
                sectorLabels = arrayOf("S", "N"),
                measurementSystem = MeasurementSystem.METRIC
        )

        now = System.currentTimeMillis()
        thresholdTime = now - parameters.alarmInterval
        beforeThresholdTime = thresholdTime - 1

        alertDataHandler = AlertDataHandler(AggregatingAlertDataMapper())
    }

    @Test
    fun testCheckWithinThresholdTimeSingleSectorAndRange1() {
        parameters = parameters.copy(sectorLabels = arrayOf("foo"))
        strike = strike.copy(timestamp = thresholdTime)
        every { location.distanceTo(any()) } returns 2500f

        val result = alertDataHandler.checkStrikes(Strikes(listOf(strike)), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull
        assertThat(sector).isEqualTo(sectorWithStrike(result))

        assertSectorAndRange(result, "foo", 2.5f, thresholdTime)
    }

    @Test
    fun testCheckWithinThresholdTimeFirstSectorAndRange1() {
        strike = strike.copy(timestamp = thresholdTime)
        every { location.distanceTo(any()) } returns 2500f
        every { location.bearingTo(any()) } returns 90f

        val result = alertDataHandler.checkStrikes(Strikes(listOf(strike)), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull
        assertThat(sector).isEqualTo(sectorWithStrike(result))

        assertSectorAndRange(result, "S", 2.5f, thresholdTime)
    }

    @Test
    fun testCheckWithinThresholdTimeSecondSectorAndRange1() {
        strike = strike.copy(timestamp = thresholdTime)
        every { location.distanceTo(any()) } returns 2500f
        every { location.bearingTo(any()) } returns 180f

        val result = alertDataHandler.checkStrikes(Strikes(listOf(strike)), location, parameters, now)

        val sector = result.sectorWithClosestStrike
        assertThat(sector).isNotNull
        assertThat(sector).isEqualTo(sectorWithStrike(result))

        assertSectorAndRange(result, "S", 2.5f, thresholdTime)
    }

    @Test
    fun testCheckWithinThresholdTimeAndOutOfAllRanges() {
        strike = strike.copy(timestamp = thresholdTime)
        every { location.distanceTo(any()) } returns 5000.1f

        val result = alertDataHandler.checkStrikes(Strikes(listOf(strike)), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
        assertThat(rangeWithStrike(result)).isNull()
    }

    @Test
    fun testCheckOutOfThresholdTimeAndWithinRange2() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        every { location.distanceTo(any()) } returns 2500.1f
        every { location.bearingTo(any()) } returns -90f

        val result = alertDataHandler.checkStrikes(Strikes(listOf(strike)), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
        assertSectorAndRange(result, "N", 5f, beforeThresholdTime)
    }

    @Test
    fun testCheckOutOfThresholdTimeAndAllRanges() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        every { location.distanceTo(any()) } returns 5000.1f

        val result = alertDataHandler.checkStrikes(Strikes(listOf(strike)), location, parameters, now)

        assertThat(result.sectorWithClosestStrike).isNull()
        assertThat(rangeWithStrike(result)).isNull()
    }

    private fun assertSectorAndRange(result: AlertResult, expectedSectorLabel: String, expectedRange: Float, expectedTime: Long) {
        val sectorWithStrike = sectorWithStrike(result)
        assertThat(sectorWithStrike).isNotNull
        if (sectorWithStrike != null) {
            assertThat(sectorWithStrike.label).isEqualTo(expectedSectorLabel)
            val rangeWithStrike = sectorWithStrike.ranges.firstOrNull { it.strikeCount > 0 }
            assertThat(rangeWithStrike).isNotNull
            if (rangeWithStrike != null) {
                assertThat(rangeWithStrike.strikeCount).isEqualTo(1)
                assertThat(rangeWithStrike.rangeMaximum).isEqualTo(expectedRange)
                assertThat(rangeWithStrike.latestStrikeTimestamp).isEqualTo(expectedTime)
            }
        }
    }

    private fun sectorWithStrike(result: AlertResult) = result.sectors.firstOrNull { !it.ranges.none { range -> range.strikeCount > 0 } }

    private fun rangeWithStrike(result: AlertResult) = result.sectors.flatMap { it.ranges }.firstOrNull { it.strikeCount > 0 }
}
