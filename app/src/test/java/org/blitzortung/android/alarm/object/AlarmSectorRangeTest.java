package org.blitzortung.android.alarm.object;

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
public class AlarmSectorRangeTest {
    
    private AlarmSectorRange alarmSectorRange;
    
    @Before
    public void setUp() {
        alarmSectorRange = new AlarmSectorRange(5.0f, 10.0f);    
    }
    
    @Test
    public void testGetRangeMinimum() {
        assertThat(alarmSectorRange.getRangeMinimum(), is(5.0f));
    }

    @Test
    public void testGetRangeMaximum() {
        assertThat(alarmSectorRange.getRangeMaximum(), is(10.0f));
    }

    @Test
    public void testGetStrokeCountInitialValue() {
        assertThat(alarmSectorRange.getStrokeCount(), is(0));
    }
    
    @Test
    public void testSetGetStrokeCount() {
        Stroke stroke = mock(Stroke.class);
        when(stroke.getMultiplicity()).thenReturn(1).thenReturn(2);
        
        alarmSectorRange.addStroke(stroke);
        assertThat(alarmSectorRange.getStrokeCount(), is(1));

        alarmSectorRange.addStroke(stroke);
        assertThat(alarmSectorRange.getStrokeCount(), is(3));
    }

    @Test
    public void testGetLatestStrokeTimestampInitialValue() {
        assertThat(alarmSectorRange.getLatestStrokeTimestamp(), is(0l));
    }
    
    @Test
    public void testGetLatestStrokeTimestamp() {
        Stroke stroke = mock(Stroke.class);
        when(stroke.getTimestamp()).thenReturn(1000l).thenReturn(5000l);
        
        alarmSectorRange.addStroke(stroke);
        assertThat(alarmSectorRange.getLatestStrokeTimestamp(), is(1000l));

        alarmSectorRange.addStroke(stroke);
        assertThat(alarmSectorRange.getLatestStrokeTimestamp(), is(5000l));
    }
    
    @Test
    public void testReset() {
        Stroke stroke = mock(Stroke.class);
        when(stroke.getTimestamp()).thenReturn(5000l);
        when(stroke.getMultiplicity()).thenReturn(2);
        
        alarmSectorRange.clearResults();
        
        assertThat(alarmSectorRange.getStrokeCount(), is(0));
        assertThat(alarmSectorRange.getLatestStrokeTimestamp(), is(0l));
    }
}
