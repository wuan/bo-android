package org.blitzortung.android.alarm;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.util.MeasurementSystem;

import java.util.HashSet;
import java.util.Set;

public class AlarmManager implements OnSharedPreferenceChangeListener, LocationListener {

    public interface AlarmListener {
        void onAlarmResult(AlarmStatus alarmStatus);

        void onAlarmClear();
    }

    private final long alarmInterval;

    private final TimerTask timerTask;

    private final LocationManager locationManager;

    private Location location;

    private boolean alarmEnabled;

    private MeasurementSystem measurementSystem;

    // VisibleForTesting
    protected final Set<AlarmListener> alarmListeners;

    private AlarmStatus alarmStatus;

    public AlarmManager(LocationManager locationManager, SharedPreferences preferences, TimerTask timerTask) {
        this.timerTask = timerTask;

        alarmListeners = new HashSet<AlarmListener>();

        this.locationManager = locationManager;

        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALARM_ENABLED);
        onSharedPreferenceChanged(preferences, PreferenceKey.MEASUREMENT_UNIT);
        alarmInterval = 600000;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case ALARM_ENABLED:
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                alarmEnabled = sharedPreferences.getBoolean(key.toString(), false) && locationManager.isProviderEnabled(locationProvider);

                if (alarmEnabled) {
                    locationManager.requestLocationUpdates(locationProvider, 0, 0, this);
                } else {
                    locationManager.removeUpdates(this);

                    for (AlarmListener alarmListener : alarmListeners) {
                        alarmListener.onAlarmClear();
                    }
                }

                timerTask.setAlarmEnabled(alarmEnabled);
                break;

            case MEASUREMENT_UNIT:
                String measurementSystemName = sharedPreferences.getString(key.toString(), MeasurementSystem.METRIC.toString());

                measurementSystem = MeasurementSystem.valueOf(measurementSystemName);
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public boolean isAlarmEnabled() {
        return alarmEnabled;
    }

    public void check(DataResult result) {

        if (alarmEnabled && result.containsRealtimeData() && location != null) {
            long now = System.currentTimeMillis();
            long thresholdTime = now - alarmInterval;
            long oldestTime = now - result.getParameters().getIntervalDuration() * 1000 * 60;

            if (alarmStatus == null || !result.isIncremental()) {
                alarmStatus = new AlarmStatus(thresholdTime, measurementSystem);
            } else {
                alarmStatus.update(thresholdTime, oldestTime, measurementSystem);
            }

            alarmStatus.check(result, location);

        } else {
            alarmStatus = null;
        }

        for (AlarmListener alarmListener : alarmListeners) {
            alarmListener.onAlarmResult(alarmStatus);
        }
    }

    public AlarmStatus getAlarmStatus() {
        return alarmStatus;
    }

    public void clearAlarmListeners() {
        alarmListeners.clear();
    }

    public void addAlarmListener(AlarmListener alarmListener) {
        alarmListeners.add(alarmListener);
    }

    public void removeAlarmListener(AlarmListener alarmView) {
        alarmListeners.remove(alarmView);
    }
}
