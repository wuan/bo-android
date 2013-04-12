package org.blitzortung.android.alarm.handler;

import android.location.Location;
import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmStatusHandlerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Mock
    private AlarmSectorHandler alarmSectorHandler;

    @Mock
    private AlarmParameters alarmParameters;

    @Mock
    private AlarmStatus alarmStatus;

    @Mock
    private AlarmSector alarmSector;

    @Mock
    private Stroke stroke;

    @Mock
    private Location location;

    private MeasurementSystem measurementSystem = MeasurementSystem.METRIC;
    
    private AlarmStatusHandler alarmStatusHandler;
    

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        alarmStatusHandler = new AlarmStatusHandler(alarmSectorHandler, alarmParameters);

        when(alarmParameters.getAlarmInterval()).thenReturn(10 * 60 * 1000l);
        when(alarmParameters.getMeasurementSystem()).thenReturn(measurementSystem);
    }

    @Test
    public void testCheckStrokesWhenBearingIsMinimumBearingOfSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(10.0f);
        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector));
        when(alarmSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alarmSector.getMaximumSectorBearing()).thenReturn(15.0f);

        final AlarmStatus returnedAlarmStatus = alarmStatusHandler.checkStrokes(alarmStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlarmStatus, is(alarmStatus));

        verify(alarmStatus, times(1)).clearResults();
        verify(alarmSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alarmSectorHandler, times(1)).checkStroke(alarmSector, stroke);
    }

    @Test
    public void testCheckStrokesWhenBearingIsNearMaximumBearingOfSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(14.999999f);
        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector));
        when(alarmSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alarmSector.getMaximumSectorBearing()).thenReturn(15.0f);

        final AlarmStatus returnedAlarmStatus = alarmStatusHandler.checkStrokes(alarmStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlarmStatus, is(alarmStatus));

        verify(alarmStatus, times(1)).clearResults();
        verify(alarmSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alarmSectorHandler, times(1)).checkStroke(alarmSector, stroke);
    }

    @Test
    public void testCheckStrokesThrowsExceptionWhenNoSectorIfFoundForBearing() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(15f);
        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector));
        when(alarmSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alarmSector.getMaximumSectorBearing()).thenReturn(15.0f);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("no sector for bearing 15.00 found");

        alarmStatusHandler.checkStrokes(alarmStatus, Lists.newArrayList(stroke), location);
    }


    @Test
    public void testCheckStrokesWhenBearingIsMinimumBearingOfSpecialSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(170f);
        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector));
        when(alarmSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alarmSector.getMaximumSectorBearing()).thenReturn(-170f);

        final AlarmStatus returnedAlarmStatus = alarmStatusHandler.checkStrokes(alarmStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlarmStatus, is(alarmStatus));

        verify(alarmStatus, times(1)).clearResults();
        verify(alarmSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alarmSectorHandler, times(1)).checkStroke(alarmSector, stroke);
    }

    @Test
    public void testCheckStrokesWhenBearingIsNearMaximumBearingOfSpecialSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(-170.00001f);
        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector));
        when(alarmSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alarmSector.getMaximumSectorBearing()).thenReturn(-170f);

        final AlarmStatus returnedAlarmStatus = alarmStatusHandler.checkStrokes(alarmStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlarmStatus, is(alarmStatus));

        verify(alarmStatus, times(1)).clearResults();
        verify(alarmSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alarmSectorHandler, times(1)).checkStroke(alarmSector, stroke);
    }

    @Test
    public void testCheckStrokesThrowsExceptionWhenNoSectorIfFoundForBearingInCaseOfSpecialSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(-170f);
        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector));
        when(alarmSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alarmSector.getMaximumSectorBearing()).thenReturn(-170f);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("no sector for bearing -170.00 found");

        alarmStatusHandler.checkStrokes(alarmStatus, Lists.newArrayList(stroke), location);
    }

    @Test
    public void testGetSectorWithClosestStroke() {
        AlarmSector alarmSector1 = mockAlarmSector("N", 50f);
        AlarmSector alarmSector2 = mockAlarmSector("NW", 30f);
        AlarmSector alarmSector3 = mockAlarmSector("S", 10f);

        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector1, alarmSector2, alarmSector3));

        final AlarmSector sectorWithClosestStroke = alarmStatusHandler.getSectorWithClosestStroke(alarmStatus);
        assertThat(sectorWithClosestStroke, is(alarmSector3));
    }
    
    @Test
    public void testGetCurrentActivity() {
        AlarmSector alarmSector1 = mockAlarmSector("foo", 50f);

        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector1));

        final AlarmResult currentActivity = alarmStatusHandler.getCurrentActivity(alarmStatus);
        
        assertThat(currentActivity.getBearingName(), is("foo"));
        assertThat(currentActivity.getClosestStrokeDistance(), is(50f));
        assertThat(currentActivity.getDistanceUnitName(), is("km"));

    }

    @Test
    public void testTextMessage() {
        AlarmSector alarmSector1 = mockAlarmSector("N", 50f);
        AlarmSector alarmSector2 = mockAlarmSector("NW", 30f);
        AlarmSector alarmSector3 = mockAlarmSector("S", 10f);
        AlarmSector alarmSector4 = mockAlarmSector("O", 30.001f);

        when(alarmStatus.getSectors()).thenReturn(Lists.newArrayList(alarmSector1, alarmSector2, alarmSector3, alarmSector4));

        final String alarmText = alarmStatusHandler.getTextMessage(alarmStatus, 30f);

        assertThat(alarmText, is("S 10km, NW 30km"));
    }

    private AlarmSector mockAlarmSector(String label, float distance) {
        AlarmSector mockedAlarmSector = mock(AlarmSector.class);
        when(mockedAlarmSector.getLabel()).thenReturn(label);
        when(mockedAlarmSector.getClosestStrokeDistance()).thenReturn(distance);
        return mockedAlarmSector;
    }


}
