package org.blitzortung.android.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class MeasurementSystemTest {

    @Test
    public void testMetricFactor() {
        float metricDistance = MeasurementSystem.METRIC.calculateDistance(123456);
        assertThat(metricDistance, is(123.456f));
    }

    @Test
    public void testMetricUnitName() {
        assertThat(MeasurementSystem.METRIC.getUnitName(), is("km"));
    }

    @Test
    public void testImperialFactor() {
        float metricDistance = MeasurementSystem.IMPERIAL.calculateDistance(2 * 1609.344f);
        assertThat(metricDistance, is(2f));
    }

    @Test
    public void testImperialUnitName() {
        assertThat(MeasurementSystem.IMPERIAL.getUnitName(), is("mi."));
    }
}
