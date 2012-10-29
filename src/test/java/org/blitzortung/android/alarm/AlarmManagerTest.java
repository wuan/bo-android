package org.blitzortung.android.alarm;

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmManagerTest {

    private AlarmManager alarmManager;

    @Mock
    private LocationManager locationManager;

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

        when(sharedPreferences.getString(Preferences.MEASUREMENT_UNIT_KEY, MeasurementSystem.METRIC.toString())).thenReturn(MeasurementSystem.METRIC.toString());

        alarmManager = spy(new AlarmManager(locationManager, sharedPreferences, timerTask));
        alarmManager.addAlarmListener(alarmListener);

        verify(sharedPreferences, times(1)).getString(Preferences.MEASUREMENT_UNIT_KEY, MeasurementSystem.METRIC.toString());
    }

    @Test
    public void testConstruct()
    {
        assertFalse(alarmManager.isAlarmEnabled());
        assertThat(alarmManager.alarmListeners.size(), is(1));
        assertThat(alarmManager.getAlarmStatus(), is(nullValue()));

        verify(sharedPreferences, times(1)).registerOnSharedPreferenceChangeListener(any(AlarmManager.class));
        verify(sharedPreferences, times(1)).getBoolean(Preferences.ALARM_ENABLED_KEY, false);
        verify(locationManager, times(1)).removeUpdates(any(AlarmManager.class));
        verify(timerTask, times(1)).setAlarmEnabled(false);

        //verify(alarmManager, times(1)).onSharedPreferenceChanged(any(SharedPreferences.class), eq(Preferences.ALARM_ENABLED_KEY));
    }

    @Test
    public void testOnSharedPreferecesChangeWithAlarmDisabled()
    {
        when(sharedPreferences.getBoolean(Preferences.ALARM_ENABLED_KEY, false)).thenReturn(false);

        alarmManager.onSharedPreferenceChanged(sharedPreferences, Preferences.ALARM_ENABLED_KEY);

        assertFalse(alarmManager.isAlarmEnabled());

        verify(locationManager, times(1)).removeUpdates(alarmManager);
        verify(alarmListener, times(1)).onAlarmClear();
        verify(timerTask, times(2)).setAlarmEnabled(false);
    }

    @Test
    public void testOnSharedPreferecesChangeWithAlarmEnabled()
    {
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);

        enableAlarm();

        assertTrue(alarmManager.isAlarmEnabled());

        verify(locationManager, times(1)).requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, alarmManager);
        verify(timerTask, times(1)).setAlarmEnabled(true);
        verify(timerTask, times(1)).setAlarmEnabled(false);
    }

    @Test
    public void testOnSharedPreferecesChangeWithAlarmEnabledAndProviderDisabled()
    {
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false);

        enableAlarm();

        assertFalse(alarmManager.isAlarmEnabled());

        verify(locationManager, times(0)).requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, alarmManager);
        verify(timerTask, times(0)).setAlarmEnabled(true);
        verify(timerTask, times(2)).setAlarmEnabled(false);
    }

    private void enableAlarm() {
        when(sharedPreferences.getBoolean(Preferences.ALARM_ENABLED_KEY, false)).thenReturn(true);

        alarmManager.onSharedPreferenceChanged(sharedPreferences, Preferences.ALARM_ENABLED_KEY);
    }

    @Test
    public void testCheckWithDisabledAlarm()
    {
        DataResult result = mock(DataResult.class);

        alarmManager.check(result);

        verify(alarmListener, times(1)).onAlarmResult(null);

    }

    @Test
    public void testCheckWithEnabledAlarm()
    {
        enableAlarm();

        DataResult result = mock(DataResult.class);

        alarmManager.check(result);

        verify(alarmListener, times(1)).onAlarmResult(null);
    }

    @Test
    public void testCheckWithEnabledAlarmAndLocationSet()
    {
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);

        Location location = mock(Location.class);
        enableAlarm();
        alarmManager.onLocationChanged(location);

        AbstractStroke stroke = mock(AbstractStroke.class);
        when(stroke.getTimestamp()).thenReturn(System.currentTimeMillis());
        when(stroke.getMultiplicity()).thenReturn(1);

        Location strokeLocation = mock(Location.class);
        when(stroke.getLocation()).thenReturn(strokeLocation);
        when(location.bearingTo(strokeLocation)).thenReturn(0f);
        when(location.distanceTo(strokeLocation)).thenReturn(500000f);

        DataResult result = mock(DataResult.class);
        when(result.getStrokes()).thenReturn(Lists.newArrayList(stroke));

        alarmManager.check(result);

        ArgumentCaptor<AlarmStatus> alarmStatusCaptor = ArgumentCaptor.forClass(AlarmStatus.class);

        verify(alarmListener, times(1)).onAlarmResult(alarmStatusCaptor.capture());

        assertThat(alarmStatusCaptor.getValue().currentActivity().getDistance(), is(500f));
        assertThat(alarmStatusCaptor.getValue().currentActivity().getBearingName(), is("N"));
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
