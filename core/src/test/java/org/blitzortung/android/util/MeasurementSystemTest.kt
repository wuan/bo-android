package org.blitzortung.android.util

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MeasurementSystemTest {
    @Test
    fun testMetricFactor() {
        val metricDistance = MeasurementSystem.METRIC.calculateDistance(123456f)
        assertThat(metricDistance).isEqualTo(123.456f)
    }

    @Test
    fun testMetricUnitName() {
        assertThat(MeasurementSystem.METRIC.unitNameString).isEqualTo(R.string.unit_km)
    }

    @Test
    fun testImperialFactor() {
        val metricDistance = MeasurementSystem.IMPERIAL.calculateDistance(2 * 1609.344f)
        assertThat(metricDistance).isEqualTo(2f)
    }

    @Test
    fun testImperialUnitName() {
        assertThat(MeasurementSystem.IMPERIAL.unitNameString).isEqualTo(R.string.unit_miles)
    }

    @Test
    fun testMetricRoundTrip() {
        val value = 123.456f

        assertThat(MeasurementSystem.METRIC.toMeters(value)).isEqualTo(123456f)
        assertThat(MeasurementSystem.METRIC.calculateDistance(MeasurementSystem.METRIC.toMeters(value)))
            .isEqualTo(value)
    }

    @Test
    fun testImperialRoundTrip() {
        val value = 2f

        assertThat(MeasurementSystem.IMPERIAL.toMeters(value)).isEqualTo(2 * 1609.344f)
        assertThat(MeasurementSystem.IMPERIAL.calculateDistance(MeasurementSystem.IMPERIAL.toMeters(value)))
            .isEqualTo(value)
    }
}
