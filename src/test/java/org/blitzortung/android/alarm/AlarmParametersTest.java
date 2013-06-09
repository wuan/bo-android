package org.blitzortung.android.alarm;

import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AlarmParametersTest {
    
    private AlarmParameters alarmParameters;
    
    @Before
    public void setUp() {
        alarmParameters = new AlarmParameters();
    }
    
    @Test
    public void testGetSectorLabels() {
        final String[] sectorLabels = alarmParameters.getSectorLabels();
        
        assertThat(sectorLabels, is(not(nullValue())));
        assertThat(sectorLabels.length, is(8));
    }

    @Test
    public void testGetRangeSteps() {
        final float[] rangeSteps = alarmParameters.getRangeSteps();

        assertThat(rangeSteps, is(not(nullValue())));
        assertThat(rangeSteps.length, is(6));
    }
    
    @Test
    public void testGetSetMeasurementSystem() {
        assertThat(alarmParameters.getMeasurementSystem(), is(nullValue()));
        
        alarmParameters.setMeasurementSystem(MeasurementSystem.METRIC);

        assertThat(alarmParameters.getMeasurementSystem(), is(MeasurementSystem.METRIC));
    }
 
}
