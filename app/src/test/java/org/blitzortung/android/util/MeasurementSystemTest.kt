package org.blitzortung.android.util

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat

@RunWith(RobolectricTestRunner::class)
class MeasurementSystemTest {

    @Test
    fun testMetricFactor() {
        val metricDistance = MeasurementSystem.METRIC.calculateDistance(123456f)
        assertThat(metricDistance, `is`(123.456f))
    }

    @Test
    fun testMetricUnitName() {
        assertThat(MeasurementSystem.METRIC.unitName, `is`("km"))
    }

    @Test
    fun testImperialFactor() {
        val metricDistance = MeasurementSystem.IMPERIAL.calculateDistance(2 * 1609.344f)
        assertThat(metricDistance, `is`(2f))
    }

    @Test
    fun testImperialUnitName() {
        assertThat(MeasurementSystem.IMPERIAL.unitName, `is`("mi."))
    }
}
