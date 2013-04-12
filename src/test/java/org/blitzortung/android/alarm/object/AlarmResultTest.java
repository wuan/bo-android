package org.blitzortung.android.alarm.object;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlarmResultTest {

    @Mock
    private AlarmSector alarmSector;
    
    private MeasurementSystem measurementSystem = MeasurementSystem.METRIC;

    private AlarmResult alarmResult;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(alarmSector.getClosestStrokeDistance()).thenReturn(4567f);

        alarmResult = new AlarmResult(alarmSector, measurementSystem);
    }

    @Test
    public void testGetSector() throws Exception {
        when(alarmSector.getLabel()).thenReturn("foo");
        assertThat(alarmResult.getBearingName(), is("foo"));
    }

    @Test
    public void testGetDistance() throws Exception {
        assertThat(alarmResult.getClosestStrokeDistance(), is(4567f));
    }
    
    @Test
    public void testGetDistanceUnit()
    {
        assertThat(alarmResult.getDistanceUnitName(), is("km"));
    }
}
