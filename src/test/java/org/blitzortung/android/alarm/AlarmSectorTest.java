package org.blitzortung.android.alarm;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.util.MeasurementSystem;
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
		alarmSector = spy(new AlarmSector(90.0f, 10000, MeasurementSystem.METRIC));
	}

    @Test
	public void testEmptyObject() throws Throwable {
		int expectedDistanceLimits = 6;
		
		assertThat(AlarmSector.getDistanceStepCount(), is(expectedDistanceLimits));
		
		for (int index=0; index < expectedDistanceLimits; index++) {
			assertThat(alarmSector.getStrokeCount(index), is(0));
			assertThat(alarmSector.getLatestTime(index), is(0l));
		}

        assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is((float) Double.POSITIVE_INFINITY));
	}

    @Test
    public void testGetBearing()
    {
        assertThat(alarmSector.getBearing(), is(90f));
    }

    @Test
	public void testCheckWithOneStroke() {
		
		int strokeMultiplicity = 2;
		float strokeDistance = 150f;
		long strokeTime = 10001;
		
		alarmSector.check(strokeDistance, strokeTime, strokeMultiplicity);
		
		int minimumIndex = 4;
		assertThat(alarmSector.getStrokeCount(minimumIndex), is(strokeMultiplicity));
		assertThat(alarmSector.getLatestTime(minimumIndex), is(strokeTime));
		assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(strokeDistance));
		
	}

    @Test
	public void testCheckWithMultipleStrokes() {
		
		alarmSector.check(200f, 1000010, 1);

		alarmSector.check(150f, 1000000, 2);
		
		alarmSector.check(450.0f, 9999999, 4);
		
		int minimumIndex = 4;
		assertThat(alarmSector.getStrokeCount(minimumIndex), is(3));
		assertThat(alarmSector.getLatestTime(minimumIndex), is(1000010l));
		assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(150f));
	}

    @Test
    public void testGetEventCount()
    {
        int eventCount[] = new int[AlarmSector.getDistanceStepCount()];

        assertThat(alarmSector.getStrokeCounts(), is(eventCount));

        alarmSector.check(10f, 15000, 1);

        eventCount[0] = 1;

        assertThat(alarmSector.getStrokeCounts(), is(eventCount));
    }

    @Test
    public void testGetLatestTimes()
    {
        long latestTimes[] = new long[AlarmSector.getDistanceStepCount()];

        assertThat(alarmSector.getTimesOfLatestStroke(), is(latestTimes));

        alarmSector.check(10f, 15000, 1);

        latestTimes[0] = 15000;

        assertThat(alarmSector.getTimesOfLatestStroke(), is(latestTimes));
    }

    @Test
    public void testGetWarnMinimumDistance()
    {
        assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(Float.POSITIVE_INFINITY));

        alarmSector.check(10f, 15000, 1);

        assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(10f));
    }

    @Test
    public void testGetWarnThresholdTime()
    {
        assertThat(alarmSector.getThresholdTime(), is(10000l));
    }

    @Test
    public void testUpdateWarnThresholdTimeRemovesOlderWarning()
    {
        alarmSector.check(10f, 25000, 1);
        assertThat(alarmSector.getStrokeCount(0), is(1));
        assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(10f));

        alarmSector.update(20000l, MeasurementSystem.METRIC);

        assertThat(alarmSector.getThresholdTime(), is(20000l));
        assertThat(alarmSector.getStrokeCount(0), is(0));
        assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(Float.POSITIVE_INFINITY));
    }

    @Test
    public void testUpdateWarnThresholdTimeRemovesOlderValues()
    {
        alarmSector.check(10f, 15000, 1);
        assertThat(alarmSector.getStrokeCount(0), is(1));
        assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(10f));

        alarmSector.update(20000l, MeasurementSystem.METRIC);

        assertThat(alarmSector.getThresholdTime(), is(20000l));
        assertThat(alarmSector.getStrokeCount(0), is(0));
        assertThat(alarmSector.getMinimumAlarmRelevantStrokeDistance(), is(Float.POSITIVE_INFINITY));
    }

}
