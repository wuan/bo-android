package org.blitzortung.android.alarm;

import android.location.Location;
import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.util.MeasurementSystem;
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

    private MeasurementSystem measurementSystem;

    private long warnThresholdTime;

    private long now;

    @Before
    public void setUp() {
        measurementSystem = MeasurementSystem.METRIC;

        now = System.currentTimeMillis();
        warnThresholdTime = now - 60000;

        alarmStatus = new AlarmStatus(warnThresholdTime, measurementSystem);
    }

    @Test
    public void testConstruct() {
        assertThat(alarmStatus.sectors.length, is(alarmStatus.getSectorCount()));

        float sectorWidth = 360f / alarmStatus.getSectorCount();
        float bearing = -180 - sectorWidth;
        for (AlarmSector sector : alarmStatus.sectors) {
            assertThat(sector.getBearing() - bearing, is(sectorWidth));
            bearing = sector.getBearing();

            assertThat(sector.getThresholdTime(), is(warnThresholdTime));
        }
    }

    @Test
    public void testUpdateWarnThresholdTime() {
        replaceSectorsWithMocks();

        long updatedWarnThresholdTime = warnThresholdTime + 10000;

        alarmStatus.update(updatedWarnThresholdTime, measurementSystem);

        for (AlarmSector sector : alarmStatus.sectors) {
            verify(sector, times(1)).update(updatedWarnThresholdTime, measurementSystem);
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

        alarmStatus.check(Lists.newArrayList(stroke), location);

        verify(location, times(1)).bearingTo(strokeLocation);

        int index = 0;
        for (AlarmSector sector : alarmStatus.sectors) {
            verify(sector, times(index == 0 ? 1 : 0)).check(stroke, location);
            index++;
        }
    }

    @Test
    public void testGetSectorWithClosestStroke()
    {
        replaceSectorsWithMocks();

        for (AlarmSector sector : alarmStatus.sectors) {
            when(sector.getMinimumAlarmRelevantStrokeDistance()).thenReturn(50000f);
        }
        when(alarmStatus.sectors[3].getMinimumAlarmRelevantStrokeDistance()).thenReturn(25000f);

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
            when(sector.getMinimumAlarmRelevantStrokeDistance()).thenReturn(50000f);
        }
        when(alarmStatus.sectors[2].getMinimumAlarmRelevantStrokeDistance()).thenReturn(11000f);

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
        assertThat(alarmStatus.getCurrentActivity(), is(nullValue()));

        replaceSectorsWithMocks();

        for (AlarmSector sector : alarmStatus.sectors) {
            when(sector.getMinimumAlarmRelevantStrokeDistance()).thenReturn(50f);
            when(sector.getDistanceUnitName()).thenReturn("km");
        }
        when(alarmStatus.sectors[1].getMinimumAlarmRelevantStrokeDistance()).thenReturn(9f);

        AlarmResult alarmResult = alarmStatus.getCurrentActivity();

        assertThat(alarmResult.getClosestStrokeDistance(), is(9f));
        assertThat(alarmResult.getBearingName(), is("SW"));
    }

    @Test
    public void testGetTextMessage()
    {
        assertThat(alarmStatus.getTextMessage(15f), is(""));

        replaceSectorsWithMocks();
        for (AlarmSector sector : alarmStatus.sectors) {
            when(sector.getMinimumAlarmRelevantStrokeDistance()).thenReturn(50f);
        }
        when(alarmStatus.sectors[2].getMinimumAlarmRelevantStrokeDistance()).thenReturn(9f);

        assertThat(alarmStatus.getTextMessage(15f), is("W 9km"));

        when(alarmStatus.sectors[5].getMinimumAlarmRelevantStrokeDistance()).thenReturn(3f);

        assertThat(alarmStatus.getTextMessage(15f), is("NO 3km, W 9km"));
    }

    private void replaceSectorsWithMocks() {
        for (int i = 0; i < alarmStatus.getSectorCount(); i++) {
            AlarmSector sector = mock(AlarmSector.class);
            when(sector.getDistanceUnitName()).thenReturn("km");
            alarmStatus.sectors[i] = sector;

        }
    }
}
