package org.blitzortung.android.alarm;

import android.content.SharedPreferences;
import android.location.Location;
import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;
import org.blitzortung.android.alarm.handler.AlarmStatusHandler;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmManagerTest {

    @Mock
    private AlarmParameters alarmParameters;

    @Mock
    private AlarmStatus alarmStatus;

    @Mock
    private AlarmResult alarmResult;

    @Mock
    private AlarmStatusHandler alarmStatusHandler;

    @Mock
    private AlarmObjectFactory alarmObjectFactory;

    @Mock
    private LocationHandler locationManager;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private Collection<Stroke> strokes;

    @Mock
    private Location location;

    @Mock
    private AlarmManager.AlarmListener alarmListener;

    final private long alarmInterval = 600000;

    private AlarmManager alarmManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(alarmParameters.getAlarmInterval()).thenReturn(alarmInterval);
        when(alarmObjectFactory.createAlarmStatus(alarmParameters)).thenReturn(alarmStatus);
        when(alarmObjectFactory.createAlarmStatusHandler(alarmParameters)).thenReturn(alarmStatusHandler);
        when(sharedPreferences.getBoolean(PreferenceKey.ALARM_ENABLED.toString(), false)).thenReturn(false);
        when(sharedPreferences.getString(PreferenceKey.MEASUREMENT_UNIT.toString(), MeasurementSystem.METRIC.toString())).thenReturn(MeasurementSystem.METRIC.toString());
        when(alarmStatusHandler.getCurrentActivity(alarmStatus)).thenReturn(alarmResult);

        alarmManager = spy(new AlarmManager(locationManager, sharedPreferences, alarmObjectFactory, alarmParameters));
        alarmManager.addAlarmListener(alarmListener);
    }

    @Test
    public void testConstruction() {
        assertFalse(alarmManager.isAlarmEnabled());
        assertThat(alarmManager.alarmListeners.size(), is(1));

        verify(sharedPreferences, times(1)).registerOnSharedPreferenceChangeListener(any(AlarmManager.class));
        verify(sharedPreferences, times(1)).getBoolean(PreferenceKey.ALARM_ENABLED.toString(), false);
        verify(locationManager, times(1)).removeUpdates(any(AlarmManager.class));
        verify(sharedPreferences, times(1)).getString(PreferenceKey.MEASUREMENT_UNIT.toString(), "METRIC");
        verify(alarmParameters, times(1)).setMeasurementSystem(MeasurementSystem.METRIC);
        verify(alarmListener, times(0)).onAlarmClear();
    }

    @Test
    public void testCheckStrokesWithAlarmDisabledAndLocationUnset() {
        alarmManager.checkStrokes(strokes);

        verify(alarmStatusHandler, times(0)).checkStrokes(alarmStatus, strokes, null);
        verify(alarmListener, times(0)).onAlarmResult(any(AlarmResult.class));
        verify(alarmListener, times(1)).onAlarmClear();
    }

    @Test
    public void testCheckStrokesWithAlarmDisabledAndLocationSet() {
        alarmManager.onLocationChanged(location);

        alarmManager.checkStrokes(strokes);

        verify(alarmStatusHandler, times(0)).checkStrokes(alarmStatus, strokes, null);
        verify(alarmListener, times(0)).onAlarmResult(any(AlarmResult.class));
        verify(alarmListener, times(1)).onAlarmClear();
    }

    @Test
    public void testCheckStrokesWithAlarmEnabledAndLocationSet() {
        when(sharedPreferences.getBoolean(PreferenceKey.ALARM_ENABLED.toString(), false)).thenReturn(true);
        alarmManager.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.ALARM_ENABLED.toString());
        alarmManager.onLocationChanged(location);

        when(alarmStatusHandler.getCurrentActivity(alarmStatus)).thenReturn(alarmResult);

        alarmManager.checkStrokes(strokes);

        verify(alarmStatusHandler, times(1)).checkStrokes(alarmStatus, strokes, location);
        verify(alarmListener, times(1)).onAlarmResult(alarmResult);
        verify(alarmListener, times(0)).onAlarmClear();
    }

    @Test
    public void testGetAlarmResult() {
        AlarmResult returnedAlarmResult = alarmManager.getAlarmResult();
        
        assertThat(returnedAlarmResult, is(nullValue()));
        verify(alarmStatusHandler, times(0)).getCurrentActivity(alarmStatus);

        makeAlarmsValid();

        returnedAlarmResult = alarmManager.getAlarmResult();
        
        assertThat(returnedAlarmResult, is(sameInstance(alarmResult)));
        verify(alarmStatusHandler, times(2)).getCurrentActivity(alarmStatus);
    }

    @Test
    public void testGetAlarmStatus() {
        AlarmStatus returnedAlarmStatus = alarmManager.getAlarmStatus();

        assertThat(returnedAlarmStatus, is(nullValue()));

        makeAlarmsValid();

        returnedAlarmStatus = alarmManager.getAlarmStatus();

        assertThat(returnedAlarmStatus, is(sameInstance(alarmStatus)));
    }

    private void makeAlarmsValid() {
        alarmManager.onLocationChanged(location);
        when(sharedPreferences.getBoolean(PreferenceKey.ALARM_ENABLED.toString(), false)).thenReturn(true);
        alarmManager.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.ALARM_ENABLED.toString());
        alarmManager.checkStrokes(Lists.<Stroke>newArrayList());
    }

    @Test
    public void testGetTextMessage() {
        when(alarmStatusHandler.getTextMessage(alarmStatus, 10.0f)).thenReturn("<message>");

        final String textMessage = alarmManager.getTextMessage(10.0f);

        verify(alarmStatusHandler, times(1)).getTextMessage(alarmStatus, 10.0f);
        assertThat(textMessage, is("<message>"));
    }

    @Test
    public void testAddAndClearAlarmListener() {
        assertThat(alarmManager.getAlarmListeners().size(), is(1));

        alarmManager.clearAlarmListeners();

        assertThat(alarmManager.getAlarmListeners().size(), is(0));

        alarmManager.addAlarmListener(mock(AlarmManager.AlarmListener.class));
        alarmManager.addAlarmListener(mock(AlarmManager.AlarmListener.class));

        assertThat(alarmManager.getAlarmListeners().size(), is(2));
    }
    
    @Test
    public void testRemoveAlarmListener() {
        assertThat(alarmManager.getAlarmListeners().size(), is(1));

        alarmManager.removeAlarmListener(alarmListener);

        assertThat(alarmManager.getAlarmListeners().size(), is(0));

        alarmManager.addAlarmListener(mock(AlarmManager.AlarmListener.class));
        alarmManager.addAlarmListener(alarmListener);

        alarmManager.removeAlarmListener(alarmListener);

        assertThat(alarmManager.getAlarmListeners().size(), is(1));
    }
    
    @Test
    public void testGetAlarmSectors() {
        final Collection<AlarmSector> alarmSectors = Lists.newArrayList();
        when(alarmStatus.getSectors()).thenReturn(alarmSectors);

        final Collection<AlarmSector> returnedAlarmSectors = alarmManager.getAlarmSectors();
        
        assertThat(returnedAlarmSectors, is(sameInstance(alarmSectors)));
        verify(alarmStatus, times(1)).getSectors();
    }
    
    @Test
    public void testGetAlarmParameters() {
        final AlarmParameters returnedAlarmParameters = alarmManager.getAlarmParameters();
        
        assertThat(returnedAlarmParameters, is(sameInstance(alarmParameters)));
    }
    
    
}
