package org.blitzortung.android.time;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class RangeHandlerTest {

    private RangeHandler rangeHandler;

    @Before
    public void setUp() {
       rangeHandler = new RangeHandler();
    }

    @Test
    public void testIfRealtimeModeIsDefault() {
        assertTrue(rangeHandler.isRealtime());
    }

    @Test
    public void testGetOffsetReturnsZeroByDefault() {
        assertThat(rangeHandler.getIntervalOffset(), is(0));
    }

    @Test
    public void testRewindInterval() {
        rangeHandler.setOffsetIncrement(15);

        assertTrue(rangeHandler.revInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(-15));

        assertTrue(rangeHandler.revInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(-30));

        for (int i=0; i < 23 * 4 - 2 - 1; i++) {
            assertTrue(rangeHandler.revInterval());
        }
        assertThat(rangeHandler.getIntervalOffset(), is(-23 * 60 + 15));

        assertTrue(rangeHandler.revInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(-23 * 60));

        assertFalse(rangeHandler.revInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(-23 * 60));
    }

    @Test
    public void testRewindIntervalWithAlignment() {
        rangeHandler.setOffsetIncrement(45);

        assertTrue(rangeHandler.revInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(-45));

        assertTrue(rangeHandler.revInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(-90));

        for (int i=0; i < 23 / 3 * 4; i++) {
            assertTrue(rangeHandler.revInterval());
        }
        assertThat(rangeHandler.getIntervalOffset(), is(-23 * 60 + 30));

        assertFalse(rangeHandler.revInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(-23 * 60 + 30));
    }

    @Test
    public void testFastforwardInterval() {
        rangeHandler.setOffsetIncrement(15);
        rangeHandler.revInterval();

        assertTrue(rangeHandler.ffwdInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(0));

        assertFalse(rangeHandler.ffwdInterval());
        assertThat(rangeHandler.getIntervalOffset(), is(0));
    }

    @Test
    public void testGoRealtime() {
        assertFalse(rangeHandler.goRealtime());

        rangeHandler.revInterval();

        assertTrue(rangeHandler.goRealtime());
        assertThat(rangeHandler.getIntervalOffset(), is(0));
    }

    @Test
    public void testGetIntervalDuration()
    {
        assertThat(rangeHandler.getIntervalDuration(), is(60));

        assertTrue(rangeHandler.setIntervalDuration(120));
        assertThat(rangeHandler.getIntervalDuration(), is(120));

        assertFalse(rangeHandler.setIntervalDuration(120));
        assertThat(rangeHandler.getIntervalDuration(), is(120));
    }


}
