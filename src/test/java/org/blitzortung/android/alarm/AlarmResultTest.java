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

        when(alarmSector.getMinimumIndex()).thenReturn(1234);
        when(alarmSector.getWarnMinimumDistance()).thenReturn(4567f);

        alarmResult = new AlarmResult(alarmSector, "FOO");
    }

    @Test
    public void testGetRange() throws Exception {
        assertThat(alarmResult.getRange(), is(1234));
    }

    @Test
    public void testGetSector() throws Exception {
        assertThat(alarmResult.getBearingName(), is("FOO"));
    }

    @Test
    public void testGetDistance() throws Exception {
        assertThat(alarmResult.getDistance(), is(4567f));
    }
}
