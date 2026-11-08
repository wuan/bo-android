package org.blitzortung.android.alert

import kotlin.math.abs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.data.Offset
import org.blitzortung.android.alert.data.AlertSector
import org.blitzortung.android.util.MeasurementSystem
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AlertZoomTargetTest {
    @Test
    fun `no alert activity frames the outermost range step`() {
        val span = AlertZoomTarget.framingMeters(NoData, parameters())

        assertThat(span).isEqualTo(500_000f)
    }

    @Test
    fun `a closest strike is framed at twice its distance`() {
        val span = AlertZoomTarget.framingMeters(activityWithClosestStrike(100f), parameters())

        assertThat(span).isCloseTo(200_000f, within(0.01f))
    }

    @Test
    fun `the target is bounded below at fifty units`() {
        val span = AlertZoomTarget.framingMeters(activityWithClosestStrike(10f), parameters())

        assertThat(span).isEqualTo(50_000f)
    }

    @Test
    fun `the target is bounded above by the outermost range step`() {
        val span = AlertZoomTarget.framingMeters(activityWithClosestStrike(1_000f), parameters())

        assertThat(span).isEqualTo(500_000f)
    }

    @Test
    fun `activity without a finite closest sector frames the outermost range step`() {
        val activity =
            LocalActivity(
                sectors = listOf(AlertSector("N", 0f, 45f, emptyList(), Float.POSITIVE_INFINITY)),
                parameters = parameters(),
                referenceTime = 0L,
            )

        val span = AlertZoomTarget.framingMeters(activity, parameters())

        assertThat(span).isEqualTo(500_000f)
    }

    @Test
    fun `imperial distances are converted from miles to metres`() {
        val span = AlertZoomTarget.framingMeters(NoData, parameters(MeasurementSystem.IMPERIAL))

        assertThat(span).isCloseTo(500 * 1609.344f, Offset.offset(0.01f))
    }

    @Test
    fun `imperial closest-strike distances are converted from miles`() {
        val span =
            AlertZoomTarget.framingMeters(
                activityWithClosestStrike(100f),
                parameters(MeasurementSystem.IMPERIAL),
            )

        assertThat(abs(span - 200 * 1609.344f)).isLessThan(0.05f)
    }

    private fun parameters(measurementSystem: MeasurementSystem = MeasurementSystem.METRIC) =
        AlertParameters(
            alarmInterval = 600_000L,
            rangeSteps = listOf(10f, 25f, 50f, 100f, 250f, 500f),
            sectorLabels = listOf("N", "E", "S", "W"),
            measurementSystem = measurementSystem,
        )

    private fun activityWithClosestStrike(distance: Float): LocalActivity =
        LocalActivity(
            sectors = listOf(AlertSector("N", 0f, 45f, emptyList(), distance)),
            parameters = parameters(),
            referenceTime = 0L,
        )
}
