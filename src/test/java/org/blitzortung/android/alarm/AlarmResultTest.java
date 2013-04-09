package org.blitzortung.android.alarm;

import com.xtremelabs.robolectric.RobolectricTestRunner;
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

    private AlarmResult alarmResult;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(alarmSector.getMinimumAlarmRelevantStrokeDistance()).thenReturn(4567f);

        alarmResult = new AlarmResult(alarmSector, "FOO");
    }

    @Test
    public void testGetSector() throws Exception {
        assertThat(alarmResult.getBearingName(), is("FOO"));
    }

    @Test
    public void testGetDistance() throws Exception {
        assertThat(alarmResult.getClosestStrokeDistance(), is(4567f));
    }
}
