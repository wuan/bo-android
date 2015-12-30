package org.blitzortung.android.alert.handler

import android.location.Location
import org.blitzortung.android.alert.AlertParameters
import org.blitzortung.android.alert.data.AlertContext
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Matchers.anyFloat
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.beans.DefaultStrike

@RunWith(RobolectricTestRunner::class)
class AlertSectorHandlerTest {

    private val measurementSystem = MeasurementSystem.METRIC

    private lateinit var strike: DefaultStrike

    @Mock
    private lateinit var location: Location

    private lateinit var strikeLocation: Location

    private var now: Long = 0

    private var thresholdTime: Long = 0

    private lateinit var alertSectorRange1: AlertSectorRange

    private lateinit var alertSectorRange2: AlertSectorRange

    private lateinit var alertParameters: AlertParameters

    private lateinit var alertSectorHandler: AlertSectorHandler

    private lateinit var alertSector: AlertSector

    private lateinit var alertContext: AlertContext

    private lateinit var sectors: List<AlertSector>

    private var beforeThresholdTime: Long = 0

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        strike = DefaultStrike(timestamp = now - 200, longitude = 12.0f, latitude = 41.0f, altitude = 0, amplitude = 20.0f)

        strikeLocation = Location("")
        strikeLocation.longitude = strike.longitude.toDouble()
        strikeLocation.latitude = strike.latitude.toDouble()

        alertSectorRange1 = AlertSectorRange(rangeMinimum = 0.0f, rangeMaximum = 2.5f)
        alertSectorRange2 = AlertSectorRange(rangeMinimum = 2.5f, rangeMaximum = 5.0f)

        alertSector = AlertSector(
                label = "SO",
                minimumSectorBearing = 12.0f,
                maximumSectorBearing = 23.5f,
                ranges = listOf(alertSectorRange1, alertSectorRange2)
        )

        sectors = listOf(alertSector)

        alertParameters = AlertParameters(
                alarmInterval = 10,
                rangeSteps = arrayOf(2.5f, 5f),
                sectorLabels = arrayOf("SO"),
                measurementSystem = MeasurementSystem.METRIC
        )

        alertContext = AlertContext(location, alertParameters, sectors)

        now = System.currentTimeMillis()
        thresholdTime = now - 10 * 60 * 1000
        beforeThresholdTime = thresholdTime - 1

        alertSectorHandler = AlertSectorHandler()
        alertSectorHandler.setCheckStrikeParameters(location, thresholdTime)
    }

    @Test
    fun testCheckWithinThresholdTimeAndRange1() {
        strike = strike.copy(timestamp = thresholdTime)

        `when`(location.distanceTo(strikeLocation)).thenReturn(2500f)

        alertSectorHandler.checkStrike(alertSector, strike, measurementSystem)

        assertThat(alertSector.closestStrikeDistance).isEqualTo(2.5f)
    }

    @Test
    fun testCheckWithinThresholdTimeAndOutOfAllRanges() {
        strike = strike.copy(timestamp = thresholdTime)
        `when`(location.distanceTo(strikeLocation)).thenReturn(5000.1f)

        alertSectorHandler.checkStrike(alertSector, strike, measurementSystem)

        assertThat(alertSector.closestStrikeDistance).isEqualTo(Float.POSITIVE_INFINITY)
    }

    @Test
    fun testCheckOutOfThresholdTimeAndWithinRange2() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        `when`(location.distanceTo(strikeLocation)).thenReturn(2500.1f)

        alertSectorHandler.checkStrike(alertSector, strike, measurementSystem)

        assertThat(alertSector.closestStrikeDistance).isEqualTo(Float.POSITIVE_INFINITY)
    }

    @Test
    fun testCheckOutOfThresholdTimeAndAllRanges() {
        strike = strike.copy(timestamp = beforeThresholdTime)
        `when`(location.distanceTo(strikeLocation)).thenReturn(5000.1f)

        alertSectorHandler.checkStrike(alertSector, strike, measurementSystem)

        assertThat(alertSector.closestStrikeDistance).isEqualTo(Float.POSITIVE_INFINITY)
    }
}
