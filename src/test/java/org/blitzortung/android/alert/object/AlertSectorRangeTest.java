package org.blitzortung.android.alert.object;

import org.blitzortung.android.data.beans.Stroke;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlertSectorRangeTest {
    
    private AlertSectorRange alertSectorRange;
    
    @Before
    public void setUp() {
        alertSectorRange = new AlertSectorRange(5.0f, 10.0f);
    }
    
    @Test
    public void testGetRangeMinimum() {
        assertThat(alertSectorRange.getRangeMinimum(), is(5.0f));
    }

    @Test
    public void testGetRangeMaximum() {
        assertThat(alertSectorRange.getRangeMaximum(), is(10.0f));
    }

    @Test
    public void testGetStrokeCountInitialValue() {
        assertThat(alertSectorRange.getStrokeCount(), is(0));
    }
    
    @Test
    public void testSetGetStrokeCount() {
        Stroke stroke = mock(Stroke.class);
        when(stroke.getMultiplicity()).thenReturn(1).thenReturn(2);
        
        alertSectorRange.addStroke(stroke);
        assertThat(alertSectorRange.getStrokeCount(), is(1));

        alertSectorRange.addStroke(stroke);
        assertThat(alertSectorRange.getStrokeCount(), is(3));
    }

    @Test
    public void testGetLatestStrokeTimestampInitialValue() {
        assertThat(alertSectorRange.getLatestStrokeTimestamp(), is(0l));
    }
    
    @Test
    public void testGetLatestStrokeTimestamp() {
        Stroke stroke = mock(Stroke.class);
        when(stroke.getTimestamp()).thenReturn(1000l).thenReturn(5000l);
        
        alertSectorRange.addStroke(stroke);
        assertThat(alertSectorRange.getLatestStrokeTimestamp(), is(1000l));

        alertSectorRange.addStroke(stroke);
        assertThat(alertSectorRange.getLatestStrokeTimestamp(), is(5000l));
    }
    
    @Test
    public void testReset() {
        Stroke stroke = mock(Stroke.class);
        when(stroke.getTimestamp()).thenReturn(5000l);
        when(stroke.getMultiplicity()).thenReturn(2);
        
        alertSectorRange.clearResults();
        
        assertThat(alertSectorRange.getStrokeCount(), is(0));
        assertThat(alertSectorRange.getLatestStrokeTimestamp(), is(0l));
    }
}
