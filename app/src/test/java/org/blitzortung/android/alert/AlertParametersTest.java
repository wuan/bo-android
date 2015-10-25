package org.blitzortung.android.alert;

import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19)
public class AlertParametersTest {
    
    private AlertParameters alertParameters;
    
    @Before
    public void setUp() {
        alertParameters = new AlertParameters();
        alertParameters.updateSectorLabels(RuntimeEnvironment.application);
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
