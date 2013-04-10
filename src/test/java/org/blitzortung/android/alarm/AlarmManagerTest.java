package org.blitzortung.android.alarm;

import android.content.SharedPreferences;
import android.location.Location;
import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmManagerTest {

    private AlarmManager alarmManager;

    @Mock
    private LocationHandler locationManager;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private TimerTask timerTask;

    @Mock
    private AlarmManager.AlarmListener alarmListener;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(sharedPreferences.getString(PreferenceKey.MEASUREMENT_UNIT.toString(), MeasurementSystem.METRIC.toString())).thenReturn(MeasurementSystem.METRIC.toString());

        alarmManager = spy(new AlarmManager(locationManager, sharedPreferences, timerTask));
        alarmManager.addAlarmListener(alarmListener);

        verify(sharedPreferences, times(1)).getString(PreferenceKey.MEASUREMENT_UNIT.toString(), MeasurementSystem.METRIC.toString());
    }

    @Test
    public void testConstruct()
    {
        assertFalse(alarmManager.isAlarmEnabled());
        assertThat(alarmManager.alarmListeners.size(), is(1));
        assertThat(alarmManager.getAlarmStatus(), is(nullValue()));

        verify(sharedPreferences, times(1)).registerOnSharedPreferenceChangeListener(any(AlarmManager.class));
        verify(sharedPreferences, times(1)).getBoolean(PreferenceKey.ALARM_ENABLED.toString(), true);
        verify(locationManager, times(1)).removeUpdates(any(AlarmManager.class));
        verify(timerTask, times(1)).setAlarmEnabled(false);

        //verify(alarmManager, times(1)).onSharedPreferenceChanged(any(SharedPreferences.class), eq(Preferences.ALARM_ENABLED_KEY));
    }

    @Test
    public void testOnSharedPreferecesChangeWithAlarmDisabled()
    {
        when(sharedPreferences.getBoolean(PreferenceKey.ALARM_ENABLED.toString(), false)).thenReturn(false);

        alarmManager.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.ALARM_ENABLED.toString());

        assertFalse(alarmManager.isAlarmEnabled());

        verify(locationManager, times(1)).removeUpdates(alarmManager);
        verify(alarmListener, times(1)).onAlarmClear();
        verify(timerTask, times(2)).setAlarmEnabled(false);
    }

    @Test
    public void testOnSharedPreferecesChangeWithAlarmEnabled()
    {
        when(locationManager.isProviderEnabled()).thenReturn(true);

        enableAlarm();

        assertTrue(alarmManager.isAlarmEnabled());

        verify(locationManager, times(1)).requestUpdates(alarmManager);
        verify(timerTask, times(1)).setAlarmEnabled(true);
        verify(timerTask, times(1)).setAlarmEnabled(false);
    }

    @Test
    public void testOnSharedPreferecesChangeWithAlarmEnabledAndProviderDisabled()
    {
        when(locationManager.isProviderEnabled()).thenReturn(false);

        enableAlarm();

        assertFalse(alarmManager.isAlarmEnabled());

        verify(locationManager, times(0)).requestUpdates(alarmManager);
        verify(timerTask, times(0)).setAlarmEnabled(true);
        verify(timerTask, times(2)).setAlarmEnabled(false);
    }

    private void enableAlarm() {
        when(sharedPreferences.getBoolean(PreferenceKey.ALARM_ENABLED.toString(), true)).thenReturn(true);
        alarmManager.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.ALARM_ENABLED.toString());
        verify(sharedPreferences, times(2)).getBoolean(PreferenceKey.ALARM_ENABLED.toString(), true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCheckWithDisabledAlarm()
    {
        Collection<Stroke> strokes = mock(Collection.class);

        alarmManager.check(strokes, false);

        verify(alarmListener, times(0)).onAlarmResult(any(AlarmStatus.class));
        verify(alarmListener, times(1)).onAlarmClear();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCheckWithEnabledAlarm()
    {
        enableAlarm();
        verify(alarmListener, times(1)).onAlarmClear();

        Collection<Stroke> strokes = mock(Collection.class);

        alarmManager.check(strokes, false);

        verify(alarmListener, times(0)).onAlarmResult(any(AlarmStatus.class));
        verify(alarmListener, times(2)).onAlarmClear();
    }

    @Test
    public void testCheckWithEnabledAlarmAndLocationSet()
    {
        when(locationManager.isProviderEnabled()).thenReturn(true);
        enableAlarm();
        
        Location location = mock(Location.class);
        alarmManager.onLocationChanged(location);

        AbstractStroke stroke = mock(AbstractStroke.class);
        when(stroke.getTimestamp()).thenReturn(System.currentTimeMillis());
        when(stroke.getMultiplicity()).thenReturn(1);

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation()).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(0f);
        when(location.distanceTo(strokeLocation)).thenReturn(500000f);

        alarmManager.check(Lists.newArrayList(stroke), true);

        ArgumentCaptor<AlarmStatus> alarmStatusCaptor = ArgumentCaptor.forClass(AlarmStatus.class);

        verify(alarmListener, times(1)).onAlarmResult(alarmStatusCaptor.capture());
        verify(alarmListener, times(1)).onAlarmClear();

        assertThat(alarmStatusCaptor.getValue().getCurrentActivity().getClosestStrokeDistance(), is(500f));
        assertThat(alarmStatusCaptor.getValue().getCurrentActivity().getBearingName(), is("N"));
    }

    @Test
    public void testClearAlarmListeners()
    {
        alarmManager.clearAlarmListeners();

        assertThat(alarmManager.alarmListeners.size(), is(0));
    }

    @Test
    public void testRemoveAlarmListener()
    {
        alarmManager.removeAlarmListener(alarmListener);

        assertThat(alarmManager.alarmListeners.size(), is(0));
    }


}
