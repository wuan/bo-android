package org.blitzortung.android.alarm;

import android.test.AndroidTestCase;

public class AlarmResultTest extends AndroidTestCase {

    private AlarmResult alarmResult;

    public void setUp() throws Exception {
        alarmResult = new AlarmResult(1000, 5, 2000.0f);
    }

    public void testGetRange() throws Exception {
        assertEquals(1000, alarmResult.getRange());
    }

    public void testGetSector() throws Exception {
        assertEquals(5, alarmResult.getSector());
    }

    public void testGetDistance() throws Exception {
        assertEquals(2000.0f, alarmResult.getDistance());
    }
}
