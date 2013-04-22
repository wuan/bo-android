package org.blitzortung.android.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;
import org.blitzortung.android.alarm.handler.AlarmStatusHandler;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.util.MeasurementSystem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AlarmManager implements OnSharedPreferenceChangeListener, LocationHandler.Listener {

    private final Vibrator vibrator;
    private final NotificationHandler notificationHandler;
    private final Context context;
    private Collection<? extends Stroke> lastStrokes;
    private int vibrationSignalDuration;
    private Uri alarmSoundNotificationSignal;

    public interface AlarmListener {
        void onAlarmResult(AlarmResult alarmResult);

        void onAlarmClear();
    }

    private final AlarmParameters alarmParameters;

    private Location location;

    private boolean alarmEnabled;

    private boolean alarmValid;

    // VisibleForTesting
    protected final Set<AlarmListener> alarmListeners;

    private final AlarmStatus alarmStatus;

    private final AlarmStatusHandler alarmStatusHandler;

    private final LocationHandler locationHandler;

    private float notificationDistanceLimit;

    private float signalingDistanceLimit;
    
    private long signalingLastTimestamp;

    public AlarmManager(LocationHandler locationHandler, SharedPreferences preferences, Context context, Vibrator vibrator, NotificationHandler notificationHandler, AlarmObjectFactory alarmObjectFactory, AlarmParameters alarmParameters) {
        this.locationHandler = locationHandler;
        this.context = context;
        this.vibrator = vibrator;
        this.notificationHandler = notificationHandler;
        this.alarmStatus = alarmObjectFactory.createAlarmStatus(alarmParameters);
        this.alarmStatusHandler = alarmObjectFactory.createAlarmStatusHandler(alarmParameters);
        this.alarmParameters = alarmParameters;


        alarmListeners = new HashSet<AlarmListener>();

        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALARM_ENABLED);
        onSharedPreferenceChanged(preferences, PreferenceKey.MEASUREMENT_UNIT);
        onSharedPreferenceChanged(preferences, PreferenceKey.NOTIFICATION_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, PreferenceKey.SIGNALING_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALARM_VIBRATION_SIGNAL);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALARM_SOUND_SIGNAL);

        alarmValid = false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case ALARM_ENABLED:
                alarmEnabled = sharedPreferences.getBoolean(key.toString(), false);

                if (alarmEnabled) {
                    locationHandler.requestUpdates(this);
                } else {
                    locationHandler.removeUpdates(this);
                    location = null;
                    broadcastClear();
                }
                break;

            case MEASUREMENT_UNIT:
                String measurementSystemName = sharedPreferences.getString(key.toString(), MeasurementSystem.METRIC.toString());

                alarmParameters.setMeasurementSystem(MeasurementSystem.valueOf(measurementSystemName));
                break;

            case NOTIFICATION_DISTANCE_LIMIT:
                notificationDistanceLimit = Float.parseFloat(sharedPreferences.getString(key.toString(), "50"));
                break;

            case SIGNALING_DISTANCE_LIMIT:
                signalingDistanceLimit = Float.parseFloat(sharedPreferences.getString(key.toString(), "25"));
                break;

            case ALARM_VIBRATION_SIGNAL:
                vibrationSignalDuration = sharedPreferences.getInt(key.toString(), 3) * 10;
                break;

            case ALARM_SOUND_SIGNAL:
                alarmSoundNotificationSignal = Uri.parse(sharedPreferences.getString(key.toString(), ""));
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;

        if (location == null) {
            invalidateAlarm();
        } else {
            if (lastStrokes != null) {
                checkStrokes(lastStrokes, true);
            }
        }
    }

    public boolean isAlarmEnabled() {
        return alarmEnabled;
    }

    public void checkStrokes(Collection<? extends Stroke> strokes, boolean areRealtimeStrokes) {
        boolean currentAlarmValid = isAlarmEnabled() && location != null && areRealtimeStrokes;
        lastStrokes = areRealtimeStrokes ? strokes : null;

        if (currentAlarmValid) {
            alarmValid = true;
            alarmStatusHandler.checkStrokes(alarmStatus, strokes, location);
            processResult(getAlarmResult());
        } else {
            invalidateAlarm();
        }
    }

    public AlarmResult getAlarmResult() {
        return alarmValid ? alarmStatusHandler.getCurrentActivity(alarmStatus) : null;
    }

    public String getTextMessage(float notificationDistanceLimit) {
        return alarmStatusHandler.getTextMessage(alarmStatus, notificationDistanceLimit);
    }

    public void addAlarmListener(AlarmListener alarmListener) {
        alarmListeners.add(alarmListener);
    }

    public Set<AlarmListener> getAlarmListeners() {
        return alarmListeners;
    }

    public void clearAlarmListeners() {
        alarmListeners.clear();
    }

    public void removeAlarmListener(AlarmListener alarmView) {
        alarmListeners.remove(alarmView);
    }

    public Collection<AlarmSector> getAlarmSectors() {
        return alarmStatus.getSectors();
    }

    public AlarmParameters getAlarmParameters() {
        return alarmParameters;
    }

    public AlarmStatus getAlarmStatus() {
        return alarmValid ? alarmStatus : null;
    }

    private void invalidateAlarm() {
        boolean previousAlarmValidState = alarmValid;
        alarmValid = false;

        if (previousAlarmValidState) {
            alarmStatus.clearResults();
            broadcastClear();
        }
    }

    private void broadcastClear() {
        for (AlarmListener alarmListener : alarmListeners) {
            alarmListener.onAlarmClear();
        }
    }

    private void broadcastResult(AlarmResult alarmResult) {
        for (AlarmListener alarmListener : alarmListeners) {
            alarmListener.onAlarmResult(alarmResult);
        }
    }

    private void processResult(AlarmResult alarmResult) {
        if (alarmResult != null) {
            long currentTimestamp = System.currentTimeMillis() / 1000;
            if (alarmResult.getClosestStrokeDistance() <= signalingDistanceLimit && signalingLastTimestamp + 15 < currentTimestamp) {
                vibrateIfEnabled();
                playSoundIfEnabled();
                signalingLastTimestamp = currentTimestamp;
            }
            
            if (alarmResult.getClosestStrokeDistance() <= notificationDistanceLimit) {
                notificationHandler.sendNotification(context.getResources().getString(R.string.activity) + ": " + getTextMessage(notificationDistanceLimit));
            } else {
                notificationHandler.clearNotification();
            }
        } else {
            notificationHandler.clearNotification();
        }
        
        broadcastResult(alarmResult);
    }

    private void vibrateIfEnabled() {
        vibrator.vibrate(vibrationSignalDuration);
    }

    private void playSoundIfEnabled() {
        if (alarmSoundNotificationSignal != null) {
            Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), alarmSoundNotificationSignal);
            r.play();
        }
    }

    public Location getCurrentLocation() {
        return location;
    }

}
