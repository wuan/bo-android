package org.blitzortung.android.alarm;

import android.location.Location;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmStatusTest {

    private AlarmStatus alarmStatus;

    long warnThresholdTime;

    long now;

    @Before
    public void setUp() {

        now = System.currentTimeMillis();
        warnThresholdTime = now - 60000;

        alarmStatus = new AlarmStatus(warnThresholdTime);
    }

    @Test
    public void testConstruct() {
        assertThat(alarmStatus.sectors.length, is(alarmStatus.getSectorCount()));

        float sectorWidth = 360f / alarmStatus.getSectorCount();
        float bearing = -180 - sectorWidth;
        for (AlarmSector sector : alarmStatus.sectors) {
            assertThat(sector.getBearing() - bearing, is(sectorWidth));
            bearing = sector.getBearing();

            assertThat(sector.getWarnThresholdTime(), is(warnThresholdTime));
        }
    }

    @Test
    public void testUpdateWarnThresholdTime() {
        replaceSectorsWithMocks();

        long updatedWarnThresholdTime = warnThresholdTime + 10000;

        alarmStatus.updateWarnThresholdTime(updatedWarnThresholdTime, 0l);

        for (AlarmSector sector : alarmStatus.sectors) {
            verify(sector, times(1)).updateWarnThresholdTime(updatedWarnThresholdTime, 0l);
        }
    }

    @Test
    public void testCheck() {
        replaceSectorsWithMocks();

        Location location = mock(Location.class);
        Location strokeLocation = mock(Location.class);
        AbstractStroke stroke = mock(AbstractStroke.class);
        when(stroke.getLocation()).thenReturn(strokeLocation);

        when(location.bearingTo(strokeLocation)).thenReturn(-180f);

        alarmStatus.check(location, stroke);

        verify(location, times(1)).bearingTo(strokeLocation);

        int index = 0;
        for (AlarmSector sector : alarmStatus.sectors) {
            verify(sector, times(index == 0 ? 1 : 0)).check(location, stroke);
            index++;
        }
    }

    @Test
    public void testReset() {
        replaceSectorsWithMocks();

        alarmStatus.reset();

        for (AlarmSector sector : alarmStatus.sectors) {
            verify(sector, times(1)).reset();
        }
    }

    @Test
    public void testGetSectorWithClosestStroke()
    {
        replaceSectorsWithMocks();

        for (AlarmSector sector : alarmStatus.sectors) {
            when(sector.getWarnMinimumDistance()).thenReturn(50000f);
        }
        when(alarmStatus.sectors[3].getWarnMinimumDistance()).thenReturn(25000f);

        assertThat(alarmStatus.getSectorWithClosestStroke(), is(3));
    }

    @Test
    public void testGetSector()
    {
        for (int i=0; i < alarmStatus.getSectorCount(); i++)
        {
            assertThat(alarmStatus.getSector(i), is(alarmStatus.sectors[i]));
        }
    }

    @Test
    public void testGetClosestStrokeDistance()
    {
        assertThat(alarmStatus.getClosestStrokeDistance(), is((Float.POSITIVE_INFINITY)));

        replaceSectorsWithMocks();
        for (AlarmSector sector : alarmStatus.sectors) {
            when(sector.getWarnMinimumDistance()).thenReturn(50000f);
        }
        when(alarmStatus.sectors[2].getWarnMinimumDistance()).thenReturn(11000f);

        assertThat(alarmStatus.getClosestStrokeDistance(), is((11000f)));
    }

    @Test
    public void testGetSectorBearing()
    {
        replaceSectorsWithMocks();

        int index=0;
        for (AlarmSector sector : alarmStatus.sectors) {

            alarmStatus.getSectorBearing(index++);

            verify(sector, times(1)).getBearing();
        }
    }

    @Test
    public void testCurrentActivity()
    {
        assertThat(alarmStatus.currentActivity(), is(nullValue()));

        replaceSectorsWithMocks();

        for (AlarmSector sector : alarmStatus.sectors) {
            when(sector.getWarnMinimumDistance()).thenReturn(50000f);
        }
        when(alarmStatus.sectors[2].getWarnMinimumDistance()).thenReturn(9000f);
        when(alarmStatus.sectors[2].getMinimumIndex()).thenReturn(12);

        AlarmResult alarmResult = alarmStatus.currentActivity();

        assertThat(alarmResult.getDistance(), is(9000f));
        assertThat(alarmResult.getSector(), is(2));
        assertThat(alarmResult.getRange(), is(12));
    }

    @Test
    public void testGetTextMessage()
    {
        assertThat(alarmStatus.getTextMessage(15000f), is(""));

        replaceSectorsWithMocks();
        for (AlarmSector sector : alarmStatus.sectors) {
            when(sector.getWarnMinimumDistance()).thenReturn(50000f);
        }
        when(alarmStatus.sectors[2].getWarnMinimumDistance()).thenReturn(9000f);

        assertThat(alarmStatus.getTextMessage(15000f), is("W 9km"));

        when(alarmStatus.sectors[5].getWarnMinimumDistance()).thenReturn(3000f);

        assertThat(alarmStatus.getTextMessage(15000f), is("NO 3km, W 9km"));
    }

    private void replaceSectorsWithMocks() {
        for (int i = 0; i < alarmStatus.getSectorCount(); i++) {
            alarmStatus.sectors[i] = mock(AlarmSector.class);
        }
    }
}
