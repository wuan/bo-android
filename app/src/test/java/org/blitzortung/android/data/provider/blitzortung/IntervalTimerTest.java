package org.blitzortung.android.data.provider.blitzortung;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class IntervalTimerTest {

    private long currentTime;

    private IntervalTimer intervalTimer;
    private long intervalLength;

    @Before
    public void setUp() throws Exception {
        intervalLength = 10 * 60 * 1000l;
        currentTime = System.currentTimeMillis();

        intervalTimer = new IntervalTimer(intervalLength);
    }

    @Test
    public void testMultipleIntervals() {
        intervalTimer.startInterval(currentTime - 3 * intervalLength);

        assertTrue(intervalTimer.hasNext());
        intervalTimer.next();
        assertTrue(intervalTimer.hasNext());
        intervalTimer.next();
        assertTrue(intervalTimer.hasNext());
        intervalTimer.next();
        assertTrue(intervalTimer.hasNext());
        intervalTimer.next();
        assertFalse(intervalTimer.hasNext());
    }


}
