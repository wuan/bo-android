package org.blitzortung.android.alert

import androidx.loader.R
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.alert.data.AlertSectorRange
import org.blitzortung.android.app.Main
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AlertLabelHandlerTest {

    @MockK
    private lateinit var alertLabel: AlertLabel

    private lateinit var alertParameters: AlertParameters

    private lateinit var uut: AlertLabelHandler

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        val activity = Robolectric.buildActivity(Main::class.java).create().get()

        alertParameters = AlertParameters(10, listOf(1.0f, 2.0f, 3.0f), listOf("N"), MeasurementSystem.METRIC)

        uut = AlertLabelHandler(alertLabel, activity)
    }

    @Test
    fun testNoLocation() {
        uut.apply(NoLocation)

        verify { alertLabel.setAlarmText("?") }
        verify { alertLabel.setAlarmTextColor(any()) }
    }

    @Test
    fun testWithFarActivity() {
        val ranges = listOf(AlertSectorRange(1.0f, 2.0f, 3, 1000))
        val sectors = listOf(AlertSector("NW", 10.0f, 30.0f, ranges, 55f))
        val localActivity = LocalActivity(sectors, alertParameters, 1000)

        uut.apply(localActivity)

        verify { alertLabel.setAlarmText("55km NW") }
        verify { alertLabel.setAlarmTextColor(0xff00ff00.toInt()) }
    }

    @Test
    fun testWithDistantActivity() {
        val ranges = listOf(AlertSectorRange(1.0f, 2.0f, 3, 1000))
        val sectors = listOf(AlertSector("NW", 10.0f, 30.0f, ranges, 25f))
        val localActivity = LocalActivity(sectors, alertParameters, 1000)

        uut.apply(localActivity)

        verify { alertLabel.setAlarmText("25km NW") }
        verify { alertLabel.setAlarmTextColor(0xffffff00.toInt()) }
    }

    @Test
    fun testWithActivity() {
        val ranges = listOf(AlertSectorRange(1.0f, 2.0f, 3, 1000))
        val sectors = listOf(AlertSector("NW", 10.0f, 30.0f, ranges, 2.3f))
        val localActivity = LocalActivity(sectors, alertParameters, 1000)

        uut.apply(localActivity)

        verify { alertLabel.setAlarmText("2km NW") }
        verify { alertLabel.setAlarmTextColor(0xffff6644.toInt()) }
    }

    @Test
    fun testVeryCloseActivity() {
        val ranges = listOf(AlertSectorRange(1.0f, 2.0f, 3, 1000))
        val sectors = listOf(AlertSector("NW", 10.0f, 30.0f, ranges, 0.09f))
        val localActivity = LocalActivity(sectors, alertParameters, 1000)

        uut.apply(localActivity)

        verify { alertLabel.setAlarmText("0km") }
        verify { alertLabel.setAlarmTextColor(0xffff6644.toInt()) }
    }

    @Test
    fun testWithoutActivity() {
        val ranges = listOf(AlertSectorRange(1.0f, 2.0f, 3, 1000))
        val sectors = listOf(AlertSector("NW", 10.0f, 30.0f, ranges, Float.POSITIVE_INFINITY))
        val localActivity = LocalActivity(sectors, alertParameters, 1000)

        uut.apply(localActivity)

        verify { alertLabel.setAlarmText("") }
        verify { alertLabel.setAlarmTextColor(0xff00ff00.toInt()) }
    }

}
