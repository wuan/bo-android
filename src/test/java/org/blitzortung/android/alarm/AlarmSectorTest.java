package org.blitzortung.android.alarm;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
public class AlarmSectorTest {

	private AlarmSector alarmSector;

    @Before
	public void setUp() {
		alarmSector = spy(new AlarmSector(90.0f, 10000));
	}

    @Test
	public void testEmptyObject() throws Throwable {
		int expectedDistanceLimits = 6;
		
		assertThat(AlarmSector.getDistanceStepCount(), is(expectedDistanceLimits));
		
		for (int index=0; index < expectedDistanceLimits; index++) {
			assertThat(alarmSector.getCount(index), is(0));
			assertThat(alarmSector.getLatestTime(index), is(0l));
			assertThat(alarmSector.getWarnMinimumDistance(), is((float) Double.POSITIVE_INFINITY));
		}
		
		assertThat(alarmSector.getMinimumIndex(), is(-1));
	}

    @Test
    public void testGetBearing()
    {
        assertThat(alarmSector.getBearing(), is(90f));
    }

    @Test
	public void testCheckWithOneStroke() {
		
		int strokeMultiplicity = 2;
		float strokeDistance = 150000f;
		long strokeTime = 10001;
		
		alarmSector.check(strokeDistance, strokeTime, strokeMultiplicity);
		
		int minimumIndex = alarmSector.getMinimumIndex();
		
		assertThat(minimumIndex, is(4));
		
		assertThat(alarmSector.getCount(minimumIndex), is(strokeMultiplicity));
		assertThat(alarmSector.getLatestTime(minimumIndex), is(strokeTime));
		assertThat(alarmSector.getWarnMinimumDistance(), is(strokeDistance));
		
	}

    @Test
	public void testCheckWithMultipleStrokes() {
		
		alarmSector.check(200000f, 1000010, 1);

		alarmSector.check(150000f, 1000000, 2);
		
		alarmSector.check(450000.0f, 9999999, 4);
		
		int minimumIndex = alarmSector.getMinimumIndex();
		
		assertThat(minimumIndex, is(4));
		
		assertThat(alarmSector.getCount(minimumIndex), is(3));
		assertThat(alarmSector.getLatestTime(minimumIndex), is(1000010l));
		assertThat(alarmSector.getWarnMinimumDistance(), is(150000f));
	}

    @Test
    public void testGetEventCount()
    {
        int eventCount[] = new int[AlarmSector.getDistanceStepCount()];

        assertThat(alarmSector.getEventCount(), is(eventCount));

        alarmSector.check(10000f, 15000, 1);

        eventCount[0] = 1;

        assertThat(alarmSector.getEventCount(), is(eventCount));
    }

    @Test
    public void testGetLatestTimes()
    {
        long latestTimes[] = new long[AlarmSector.getDistanceStepCount()];

        assertThat(alarmSector.getLatestTimes(), is(latestTimes));

        alarmSector.check(10000f, 15000, 1);

        latestTimes[0] = 15000;

        assertThat(alarmSector.getLatestTimes(), is(latestTimes));
    }

    @Test
    public void testGetWarnMinimumDistance()
    {
        assertThat(alarmSector.getWarnMinimumDistance(), is(Float.POSITIVE_INFINITY));

        alarmSector.check(10000f, 15000, 1);

        assertThat(alarmSector.getWarnMinimumDistance(), is(10000f));
    }

    @Test
    public void testGetWarnThresholdTime()
    {
        assertThat(alarmSector.getWarnThresholdTime(), is(10000l));
    }

    @Test
    public void testUpdateWarnThresholdTimeRemovesOlderValues()
    {
        alarmSector.check(10000f, 15000, 1);
        assertThat(alarmSector.getCount(0), is(1));

        alarmSector.updateWarnThresholdTime(20000l);

        assertThat(alarmSector.getWarnThresholdTime(), is(20000l));
        assertThat(alarmSector.getCount(0), is(0));
    }

    @Test
    public void testUpdateWarnThresholdTimeKeepsNewerValues()
    {
        alarmSector.check(10000f, 25000, 1);
        assertThat(alarmSector.getCount(0), is(1));

        alarmSector.updateWarnThresholdTime(20000l);

        assertThat(alarmSector.getWarnThresholdTime(), is(20000l));
        assertThat(alarmSector.getCount(0), is(1));
    }

}
