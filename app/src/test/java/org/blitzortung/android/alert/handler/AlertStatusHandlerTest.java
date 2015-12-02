package org.blitzortung.android.alert.handler;

import android.location.Location;

import com.google.common.collect.Lists;

import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.AlertResult;
import org.blitzortung.android.alert.data.AlertSector;
import org.blitzortung.android.alert.data.AlertStatus;
import org.blitzortung.android.data.beans.Strike;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private Strike strike;

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
    public void testCheckStrikesWhenBearingIsMinimumBearingOfSector() {

        Location strikeLocation = mock(Location.class);
        when(strike.updateLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.bearingTo(strikeLocation)).thenReturn(10.0f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(15.0f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrikes(alertStatus, Lists.newArrayList(strike), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrikeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStrike(alertSector, strike);
    }

    @Test
    public void testCheckStrikesWhenBearingIsNearMaximumBearingOfSector() {

        Location strikeLocation = mock(Location.class);
        when(strike.updateLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.bearingTo(strikeLocation)).thenReturn(14.999999f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(15.0f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrikes(alertStatus, Lists.newArrayList(strike), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrikeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStrike(alertSector, strike);
    }

    @Test
    public void testCheckStrikesThrowsExceptionWhenNoSectorIfFoundForBearing() {

        Location strikeLocation = mock(Location.class);
        when(strike.updateLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.bearingTo(strikeLocation)).thenReturn(15f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(10.0f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(15.0f);

        alertStatusHandler.checkStrikes(alertStatus, Lists.newArrayList(strike), location);

        verify(alertSectorHandler, times(1)).checkStrike(null, strike);
    }

    @Test
    public void testCheckStrikesWhenBearingIsMinimumBearingOfSpecialSector() {

        Location strikeLocation = mock(Location.class);
        when(strike.updateLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.bearingTo(strikeLocation)).thenReturn(170f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(-170f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrikes(alertStatus, Lists.newArrayList(strike), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrikeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStrike(alertSector, strike);
    }

    @Test
    public void testCheckStrikesWhenBearingIsNearMaximumBearingOfSpecialSector() {

        Location strikeLocation = mock(Location.class);
        when(strike.updateLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.bearingTo(strikeLocation)).thenReturn(-170.00001f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(-170f);

        final AlertStatus returnedAlertStatus = alertStatusHandler.checkStrikes(alertStatus, Lists.newArrayList(strike), location);

        assertThat(returnedAlertStatus, is(alertStatus));

        verify(alertStatus, times(1)).clearResults();
        verify(alertSectorHandler, times(1)).setCheckStrikeParameters(eq(location), anyLong());
        verify(alertSectorHandler, times(1)).checkStrike(alertSector, strike);
    }

    @Test
    public void testCheckStrikesThrowsExceptionWhenNoSectorIfFoundForBearingInCaseOfSpecialSector() {

        Location strikeLocation = mock(Location.class);
        when(strike.updateLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.bearingTo(strikeLocation)).thenReturn(-170f);
        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector));
        when(alertSector.getMinimumSectorBearing()).thenReturn(170f);
        when(alertSector.getMaximumSectorBearing()).thenReturn(-170f);

        alertStatusHandler.checkStrikes(alertStatus, Lists.newArrayList(strike), location);

        verify(alertSectorHandler, times(1)).checkStrike(null, strike);
    }

    @Test
    public void testGetSectorWithClosestStrike() {
        AlertSector alertSector1 = mockAlarmSector("N", 50f);
        AlertSector alertSector2 = mockAlarmSector("NW", 30f);
        AlertSector alertSector3 = mockAlarmSector("S", 10f);

        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector1, alertSector2, alertSector3));

        final AlertSector sectorWithClosestStrike = alertStatusHandler.getSectorWithClosestStrike(alertStatus);
        assertThat(sectorWithClosestStrike, is(alertSector3));
    }

    @Test
    public void testGetCurrentActivity() {
        AlertSector alertSector1 = mockAlarmSector("foo", 50f);

        when(alertStatus.getSectors()).thenReturn(Lists.newArrayList(alertSector1));

        final AlertResult currentActivity = alertStatusHandler.getCurrentActivity(alertStatus);

        assertThat(currentActivity.getBearingName(), is("foo"));
        assertThat(currentActivity.getClosestStrikeDistance(), is(50f));
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
        when(mockedAlertSector.getClosestStrikeDistance()).thenReturn(distance);
        return mockedAlertSector;
    }


}
