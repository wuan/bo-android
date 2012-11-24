package org.blitzortung.android.data;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ParametersTest {

    private Parameters parameters;

    @Before
    public void setUp() {
        parameters = new Parameters();
        parameters.setOffsetIncrement(15);
        parameters.setIntervalDuration(60);
    }

    @Test
    public void testIfRealtimeModeIsDefault() {
        assertTrue(parameters.isRealtime());
    }

    @Test
    public void testGetOffsetReturnsZeroByDefault() {
        assertThat(parameters.getIntervalOffset(), is(0));
    }

    @Test
    public void testRewindInterval() {
        assertTrue(parameters.revInterval());
        assertThat(parameters.getIntervalOffset(), is(-15));

        assertTrue(parameters.revInterval());
        assertThat(parameters.getIntervalOffset(), is(-30));

        for (int i = 0; i < 23 * 4 - 2 - 1; i++) {
            assertTrue(parameters.revInterval());
        }
        assertThat(parameters.getIntervalOffset(), is(-23 * 60 + 15));

        assertTrue(parameters.revInterval());
        assertThat(parameters.getIntervalOffset(), is(-23 * 60));

        assertFalse(parameters.revInterval());
        assertThat(parameters.getIntervalOffset(), is(-23 * 60));
    }

    @Test
    public void testRewindIntervalWithAlignment() {
        parameters.setOffsetIncrement(45);

        assertTrue(parameters.revInterval());
        assertThat(parameters.getIntervalOffset(), is(-45));

        assertTrue(parameters.revInterval());
        assertThat(parameters.getIntervalOffset(), is(-90));

        for (int i = 0; i < 23 / 3 * 4; i++) {
            assertTrue(parameters.revInterval());
        }
        assertThat(parameters.getIntervalOffset(), is(-23 * 60 + 30));

        assertFalse(parameters.revInterval());
        assertThat(parameters.getIntervalOffset(), is(-23 * 60 + 30));
    }

    @Test
    public void testFastforwardInterval() {
        parameters.setOffsetIncrement(15);
        parameters.revInterval();

        assertTrue(parameters.ffwdInterval());
        assertThat(parameters.getIntervalOffset(), is(0));

        assertFalse(parameters.ffwdInterval());
        assertThat(parameters.getIntervalOffset(), is(0));
    }

    @Test
    public void testGoRealtime() {
        parameters.setOffsetIncrement(15);

        assertFalse(parameters.goRealtime());

        parameters.revInterval();

        assertTrue(parameters.goRealtime());
        assertThat(parameters.getIntervalOffset(), is(0));
    }

    @Test
    public void testGetIntervalDuration() {
        assertThat(parameters.getIntervalDuration(), is(60));

        assertTrue(parameters.setIntervalDuration(120));
        assertThat(parameters.getIntervalDuration(), is(120));

        assertFalse(parameters.setIntervalDuration(120));
        assertThat(parameters.getIntervalDuration(), is(120));
    }


}
