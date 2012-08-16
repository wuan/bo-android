package org.blitzortung.android.alarm;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AlarmSectorTest {

	private AlarmSector alarmSector;

    @Before
	public void setUp() {
		alarmSector = new AlarmSector(90.0f, 10000);
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

}
