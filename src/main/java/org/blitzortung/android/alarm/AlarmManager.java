package org.blitzortung.android.alarm;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.util.MeasurementSystem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AlarmManager implements OnSharedPreferenceChangeListener, LocationHandler.Listener {

    private Collection<? extends Stroke> strokes;
    private boolean alarmActive;

    public interface AlarmListener {
        void onAlarmResult(AlarmStatus alarmStatus);

        void onAlarmClear();
    }

    private final long alarmInterval;

    private final TimerTask timerTask;

    private final LocationHandler locationHandler;

    private Location location;

    private boolean alarmEnabled;

    private MeasurementSystem measurementSystem;

    // VisibleForTesting
    protected final Set<AlarmListener> alarmListeners;

    private AlarmStatus alarmStatus;

    public AlarmManager(LocationHandler locationHandler, SharedPreferences preferences, TimerTask timerTask) {
        this.timerTask = timerTask;

        alarmListeners = new HashSet<AlarmListener>();

        this.locationHandler = locationHandler;

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
                alarmEnabled = sharedPreferences.getBoolean(key.toString(), true) && locationHandler.isProviderEnabled();

                if (alarmEnabled) {
                    locationHandler.requestUpdates(this);
                } else {
                    locationHandler.removeUpdates(this);

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
        
        check(strokes, alarmActive);
    }

    public boolean isAlarmEnabled() {
        return alarmEnabled;
    }

    public void check(Collection<? extends Stroke> strokes, boolean alarmActive) {
        this.strokes = strokes;
        this.alarmActive = alarmActive;

        if (alarmEnabled && alarmActive && strokes != null && location != null) {
            long now = System.currentTimeMillis();
            long thresholdTime = now - alarmInterval;

            if (alarmStatus == null) {
                alarmStatus = new AlarmStatus(thresholdTime, measurementSystem);
            } else {
                alarmStatus.update(thresholdTime, measurementSystem);
            }

            alarmStatus.check(strokes, location);
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
