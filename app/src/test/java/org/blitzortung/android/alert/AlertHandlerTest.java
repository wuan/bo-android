package org.blitzortung.android.alert;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.Vibrator;

import com.google.common.collect.Lists;

import org.blitzortung.android.alert.event.AlertEvent;
import org.blitzortung.android.alert.event.AlertResultEvent;
import org.blitzortung.android.alert.factory.AlertObjectFactory;
import org.blitzortung.android.alert.handler.AlertStatusHandler;
import org.blitzortung.android.alert.data.AlertSector;
import org.blitzortung.android.alert.data.AlertStatus;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.beans.Strike;
import org.blitzortung.android.location.LocationEvent;
import org.blitzortung.android.location.LocationHandler;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlertHandlerTest {

    @Mock
    private AlertParameters alertParameters;

    @Mock
    private AlertStatus alertStatus;

    @Mock
    private AlertResult alertResult;

    @Mock
    private AlertStatusHandler alertStatusHandler;

    @Mock
    private AlertObjectFactory alertObjectFactory;

    @Mock
    private LocationHandler locationManager;

    @Mock
    private SharedPreferences sharedPreferences;
    
    @Mock
    private Context context;
    
    @Mock 
    private Vibrator vibrator;
    
    @Mock
    private NotificationHandler notificationHandler;

    @Mock
    private Collection<Strike> strikes;

    @Mock
    private Location location;

    @Mock
    private Consumer<AlertEvent> alertEventConsumer;

    private AlertHandler alertHandler;
    
    private Resources resources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        resources = RuntimeEnvironment.application.getResources();

        long alarmInterval = 600000;
        when(alertParameters.getAlarmInterval()).thenReturn(alarmInterval);
        when(alertObjectFactory.createAlarmStatus(alertParameters)).thenReturn(alertStatus);
        when(alertObjectFactory.createAlarmStatusHandler(alertParameters)).thenReturn(alertStatusHandler);
        when(sharedPreferences.getBoolean(PreferenceKey.ALERT_ENABLED.toString(), false)).thenReturn(false);
        when(sharedPreferences.getString(PreferenceKey.MEASUREMENT_UNIT.toString(), MeasurementSystem.METRIC.toString())).thenReturn(MeasurementSystem.METRIC.toString());
        when(sharedPreferences.getString(PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT.toString(), "50")).thenReturn("50");
        when(sharedPreferences.getString(PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT.toString(), "25")).thenReturn("25");
        when(sharedPreferences.getInt(PreferenceKey.ALERT_VIBRATION_SIGNAL.toString(), 3)).thenReturn(3);
        when(sharedPreferences.getString(PreferenceKey.ALERT_SOUND_SIGNAL.toString(), "")).thenReturn("");
        when(alertStatusHandler.getCurrentActivity(alertStatus)).thenReturn(alertResult);
        when(context.getResources()).thenReturn(resources);
        
        alertHandler = new AlertHandler(locationManager, sharedPreferences, context, vibrator, notificationHandler, alertObjectFactory, alertParameters);
        alertHandler.setAlertEventConsumer(alertEventConsumer);
    }

    @Test
    public void testConstruction() {
        assertFalse(alertHandler.isAlertEnabled());

        verify(sharedPreferences, times(1)).registerOnSharedPreferenceChangeListener(any(AlertHandler.class));
        verify(sharedPreferences, times(1)).getBoolean(PreferenceKey.ALERT_ENABLED.toString(), false);
        verify(locationManager, times(1)).removeUpdates(any(Consumer.class));
        verify(sharedPreferences, times(1)).getString(PreferenceKey.MEASUREMENT_UNIT.toString(), "METRIC");
        verify(alertParameters, times(1)).setMeasurementSystem(MeasurementSystem.METRIC);
        verify(alertEventConsumer, times(0)).consume(AlertHandler.ALERT_CANCEL_EVENT);
    }

    @Test
    public void testCheckStrikesWithAlarmDisabledAndLocationUnsetWhenAlarmWasNotActiveBefore() {
        alertHandler.checkStrikes(strikes);

        verify(alertStatusHandler, times(0)).checkStrikes(alertStatus, strikes, null);
        verifyZeroInteractions(alertEventConsumer);
    }

    @Test
    public void testCheckStrikesWithAlarmDisabledAndLocationUnset() {
        makeAlarmsValid();
        verify(alertEventConsumer, times(1)).consume(any(AlertResultEvent.class));
        verify(alertEventConsumer, times(0)).consume(AlertHandler.ALERT_CANCEL_EVENT);

        alertHandler.getLocationEventConsumer().consume(new LocationEvent(null));
        alertHandler.checkStrikes(strikes);

        verify(alertStatusHandler, times(0)).checkStrikes(alertStatus, strikes, null);
        verify(alertEventConsumer, times(1)).consume(any(AlertResultEvent.class));
        verify(alertEventConsumer, times(1)).consume(AlertHandler.ALERT_CANCEL_EVENT);
    }

    @Test
    public void testCheckStrikesWithAlarmDisabledAndLocationSet() {
        makeAlarmsValid();
        enableAlarmInPrefs(false);
        verify(alertEventConsumer, times(1)).consume(any(AlertResultEvent.class));
        verify(alertEventConsumer, times(1)).consume(AlertHandler.ALERT_CANCEL_EVENT);

        alertHandler.checkStrikes(strikes);

        verify(alertStatusHandler, times(0)).checkStrikes(alertStatus, strikes, null);
        verify(alertEventConsumer, times(1)).consume(any(AlertResultEvent.class));
        verify(alertEventConsumer, times(2)).consume(AlertHandler.ALERT_CANCEL_EVENT);
    }

    @Test
    public void testCheckStrikesWithAlarmEnabledAndLocationSet() {
        enableAlarmInPrefs(true);
        alertHandler.getLocationEventConsumer().consume(new LocationEvent(location));

        when(alertStatusHandler.getCurrentActivity(alertStatus)).thenReturn(alertResult);

        alertHandler.checkStrikes(strikes);

        verify(alertStatusHandler, times(1)).checkStrikes(alertStatus, strikes, location);
        verify(alertEventConsumer, times(1)).consume(any(AlertResultEvent.class));
        verify(alertEventConsumer, times(0)).consume(AlertHandler.ALERT_CANCEL_EVENT);
    }

    @Test
    public void testGetAlarmResult() {
        AlertResult returnedAlertResult = alertHandler.getAlarmResult();
        
        assertThat(returnedAlertResult, is(nullValue()));
        verify(alertStatusHandler, times(0)).getCurrentActivity(alertStatus);

        makeAlarmsValid();

        returnedAlertResult = alertHandler.getAlarmResult();
        
        assertThat(returnedAlertResult, is(sameInstance(alertResult)));
        verify(alertStatusHandler, times(2)).getCurrentActivity(alertStatus);
    }

    @Test
    public void testGetAlarmStatus() {
        AlertStatus returnedAlertStatus = alertHandler.getAlertStatus();

        assertThat(returnedAlertStatus, is(nullValue()));

        makeAlarmsValid();

        returnedAlertStatus = alertHandler.getAlertStatus();

        assertThat(returnedAlertStatus, is(sameInstance(alertStatus)));
    }

    private void makeAlarmsValid() {
        alertHandler.getLocationEventConsumer().consume(new LocationEvent(location));
        enableAlarmInPrefs(true);
        alertHandler.checkStrikes(Lists.<Strike>newArrayList());
    }

    private void enableAlarmInPrefs(boolean alarmEnabled) {
        when(sharedPreferences.getBoolean(PreferenceKey.ALERT_ENABLED.toString(), false)).thenReturn(alarmEnabled);
        alertHandler.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.ALERT_ENABLED.toString());
    }

    @Test
    public void testGetTextMessage() {
        when(alertStatusHandler.getTextMessage(alertStatus, 10.0f)).thenReturn("<message>");

        final String textMessage = alertHandler.getTextMessage(10.0f);

        verify(alertStatusHandler, times(1)).getTextMessage(alertStatus, 10.0f);
        assertThat(textMessage, is("<message>"));
    }

    @Test
    public void testGetAlarmSectors() {
        final Collection<AlertSector> alertSectors = Lists.newArrayList();
        when(alertStatus.getSectors()).thenReturn(alertSectors);

        final Collection<AlertSector> returnedAlertSectors = alertHandler.getAlarmSectors();
        
        assertThat(returnedAlertSectors, is(sameInstance(alertSectors)));
        verify(alertStatus, times(1)).getSectors();
    }
    
    @Test
    public void testGetAlarmParameters() {
        final AlertParameters returnedAlertParameters = alertHandler.getAlertParameters();
        
        assertThat(returnedAlertParameters, is(sameInstance(alertParameters)));
    }
    
    
}
