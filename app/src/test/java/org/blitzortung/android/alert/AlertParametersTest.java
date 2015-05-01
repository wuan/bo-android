package org.blitzortung.android.alert;

import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AlertParametersTest {
    
    private AlertParameters alertParameters;
    
    @Before
    public void setUp() {
        alertParameters = new AlertParameters();
        alertParameters.updateSectorLabels(Robolectric.application);
    }
    
    @Test
    public void testGetSectorLabels() {
        final String[] sectorLabels = alertParameters.getSectorLabels();
        
        assertThat(sectorLabels, is(not(nullValue())));
        assertThat(sectorLabels.length, is(8));
    }

    @Test
    public void testGetRangeSteps() {
        final float[] rangeSteps = alertParameters.getRangeSteps();

        assertThat(rangeSteps, is(not(nullValue())));
        assertThat(rangeSteps.length, is(6));
    }
    
    @Test
    public void testGetSetMeasurementSystem() {
        assertThat(alertParameters.getMeasurementSystem(), is(nullValue()));
        
        alertParameters.setMeasurementSystem(MeasurementSystem.METRIC);

        assertThat(alertParameters.getMeasurementSystem(), is(MeasurementSystem.METRIC));
    }
 
}
