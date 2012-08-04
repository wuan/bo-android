package org.blitzortung.android.alarm;

import android.test.AndroidTestCase;

public class AlarmSectorTest extends AndroidTestCase {

	private AlarmSector alarmSector;
	
	public void setUp() {
		alarmSector = new AlarmSector(90.0f, 10000);
	}
	
	public void testEmptyObject() throws Throwable {
		int expectedDistanceLimits = 6;
		
		assertEquals(expectedDistanceLimits, AlarmSector.getDistanceStepCount());
		
		for (int index=0; index < expectedDistanceLimits; index++) {
			assertEquals(0, alarmSector.getCount(index));
			assertEquals(0, alarmSector.getLatestTime(index));
			assertEquals((float) Double.POSITIVE_INFINITY, alarmSector.getWarnMinimumDistance());
		}
		
		assertEquals(-1, alarmSector.getMinimumIndex());
	}
	
	public void testAlarmSectorWithOneStroke() {
		
		int strokeMultiplicity = 2;
		float strokeDistance = 150000f;
		long strokeTime = 10001;
		
		alarmSector.check(strokeDistance, strokeTime, strokeMultiplicity);
		
		int minimumIndex = alarmSector.getMinimumIndex();
		
		assertEquals(4, minimumIndex);
		
		assertEquals(strokeMultiplicity, alarmSector.getCount(minimumIndex));
		assertEquals(strokeTime, alarmSector.getLatestTime(minimumIndex));
		assertEquals(strokeDistance, alarmSector.getWarnMinimumDistance());
		
	}
	
	public void testAlarmSectorWithMultipleStrokes() {
		
		alarmSector.check(200000f, 1000010, 1);

		alarmSector.check(150000f, 1000000, 2);
		
		alarmSector.check(450000.0f, 9999999, 4);
		
		int minimumIndex = alarmSector.getMinimumIndex();
		
		assertEquals(4, minimumIndex);
		
		assertEquals(3, alarmSector.getCount(minimumIndex));
		assertEquals(1000000, alarmSector.getLatestTime(minimumIndex));
		assertEquals(150000f, alarmSector.getWarnMinimumDistance());
		
	}
}
