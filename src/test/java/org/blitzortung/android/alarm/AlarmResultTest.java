package org.blitzortung.android.alarm;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AlarmResultTest {

    private AlarmResult alarmResult;

    @Before
    public void setUp() throws Exception {
        alarmResult = new AlarmResult(1000, 5, 2000.0f);
    }

    @Test
    public void testGetRange() throws Exception {
        assertThat(alarmResult.getRange(), is(1000));
    }

    public void testGetSector() throws Exception {
        assertThat(alarmResult.getSector(), is(5));
    }

    public void testGetDistance() throws Exception {
        assertThat(alarmResult.getDistance(), is(2000f));
    }
}
