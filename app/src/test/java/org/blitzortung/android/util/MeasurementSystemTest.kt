package org.blitzortung.android.util

import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.R
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
}
