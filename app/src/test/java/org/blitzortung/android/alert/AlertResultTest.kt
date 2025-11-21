package org.blitzortung.android.alert

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.app.R
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AlertResultTest {
    private lateinit var alertParameters: AlertParameters

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        val rangeSteps = listOf(10f, 25f, 50f, 100f, 250f, 500f)
        val alarmInterval = 10 * 60 * 1000L
        val sectorLabels = context.resources.getStringArray(R.array.direction_names).toList()
        alertParameters = AlertParameters(alarmInterval, rangeSteps, sectorLabels, MeasurementSystem.METRIC)
    }

    @Test
    fun emptySectorsResults() {
        val uut = Alarm(emptyList(), alertParameters, System.currentTimeMillis())

        assertThat(uut.closestStrikeDistance).isInfinite()
        assertThat(uut.bearingName).isEqualTo("n/a")
        assertThat(uut.sectorWithClosestStrike).isNull()
    }

    @Test
    fun singleSectorResults() {
        val uut =
            Alarm(
                listOf(AlertSector("foo", 1.0f, 2.0f, emptyList(), 10.0f)),
                alertParameters,
                System.currentTimeMillis(),
            )

        assertThat(uut.closestStrikeDistance).isEqualTo(10.0f)
        assertThat(uut.bearingName).isEqualTo("foo")
        assertThat(uut.sectorWithClosestStrike).isNotNull
    }
}
