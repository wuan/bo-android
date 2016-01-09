package org.blitzortung.android.alert.handler;

import android.location.Location;
import com.google.common.collect.Lists;
import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.AlertResult;
import org.blitzortung.android.alert.object.AlertSector;
import org.blitzortung.android.alert.object.AlertStatus;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlertStatusHandlerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Mock
    private AlertSectorHandler alertSectorHandler;

    @Mock
    private AlertParameters alertParameters;

    @Mock
    private AlertStatus alertStatus;

    @Mock
    private AlertSector alertSector;

    @Mock
    private Stroke stroke;

    @Mock
    private Location location;

    private MeasurementSystem measurementSystem = MeasurementSystem.METRIC;
    
    private AlertStatusHandler alertStatusHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        alertStatusHandler = new AlertStatusHandler(alertSectorHandler, alertParameters);

        when(alertParameters.getAlarmInterval()).thenReturn(10 * 60 * 1000l);
        when(alertParameters.getMeasurementSystem()).thenReturn(measurementSystem);
    }

    @Test
    public void testCheckStrokesWhenBearingIsMinimumBearingOfSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(10.0f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(15.0f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrokes(alertStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStroke(alertSector, stroke);
    }

    @Test
    public void testCheckStrokesWhenBearingIsNearMaximumBearingOfSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(14.999999f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(15.0f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrokes(alertStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStroke(alertSector, stroke);
    }

    @Test
    public void testCheckStrokesThrowsExceptionWhenNoSectorIfFoundForBearing() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(15f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(15.0f);

        alertStatusHandler.checkStrokes(alertStatus, Lists.newArrayList(stroke), location);

        verify(alertSectorHandler, times(1)).checkStroke(null, stroke);
    }

    @Test
    public void testCheckStrokesWhenBearingIsMinimumBearingOfSpecialSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(170f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(-170f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrokes(alertStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStroke(alertSector, stroke);
    }

    @Test
    public void testCheckStrokesWhenBearingIsNearMaximumBearingOfSpecialSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(-170.00001f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(-170f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrokes(alertStatus, Lists.newArrayList(stroke), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrokeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStroke(alertSector, stroke);
    }

    @Test
    public void testCheckStrokesThrowsExceptionWhenNoSectorIfFoundForBearingInCaseOfSpecialSector() {

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(-170f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(-170f);

        alertStatusHandler.checkStrokes(alertStatus, Lists.newArrayList(stroke), location);

        verify(alertSectorHandler, times(1)).checkStroke(null, stroke);
    }

    @Test
    public void testGetSectorWithClosestStroke() {
        AlertSector alertSector1 = mockAlarmSector("N", 50f);
        AlertSector alertSector2 = mockAlarmSector("NW", 30f);
        AlertSector alertSector3 = mockAlarmSector("S", 10f);

        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector1, alertSector2, alertSector3));

        final AlertSector sectorWithClosestStroke = alertStatusHandler.getSectorWithClosestStroke(alertStatus);
        assertThat(sectorWithClosestStroke, is(alertSector3));
    }
    
    @Test
    public void testGetCurrentActivity() {
        AlertSector alertSector1 = mockAlarmSector("foo", 50f);

        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector1));

        final AlertResult currentActivity = alertStatusHandler.getCurrentActivity(alertStatus);
        
        assertThat(currentActivity.getBearingName(), is("foo"));
        assertThat(currentActivity.getClosestStrokeDistance(), is(50f));
        assertThat(currentActivity.getDistanceUnitName(), is("km"));

    }

    @Test
    public void testTextMessage() {
        AlertSector alertSector1 = mockAlarmSector("N", 50f);
        AlertSector alertSector2 = mockAlarmSector("NW", 30f);
        AlertSector alertSector3 = mockAlarmSector("S", 10f);
        AlertSector alertSector4 = mockAlarmSector("O", 30.001f);

        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector1, alertSector2, alertSector3, alertSector4));

        final String alarmText = alertStatusHandler.getTextMessage(alertStatus, 30f);

        assertThat(alarmText, is("S 10km, NW 30km"));
    }

    private AlertSector mockAlarmSector(String label, float distance) {
        AlertSector mockedAlertSector = mock(AlertSector.class);
        when(mockedAlertSector.getLabel()).thenReturn(label);
        when(mockedAlertSector.getClosestStrokeDistance()).thenReturn(distance);
        return mockedAlertSector;
    }


}
