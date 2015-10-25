package org.blitzortung.android.alert;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.annimon.stream.function.Consumer;

import org.blitzortung.android.alert.event.AlertResultEvent;
import org.blitzortung.android.alert.event.AlertCancelEvent;
import org.blitzortung.android.alert.event.AlertEvent;
import org.blitzortung.android.alert.factory.AlertObjectFactory;
import org.blitzortung.android.alert.handler.AlertStatusHandler;
import org.blitzortung.android.alert.data.AlertSector;
import org.blitzortung.android.alert.data.AlertStatus;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.data.beans.Strike;
import org.blitzortung.android.data.provider.result.ClearDataEvent;
import org.blitzortung.android.data.provider.result.DataEvent;
import org.blitzortung.android.data.provider.result.ResultEvent;
import org.blitzortung.android.location.LocationEvent;
import org.blitzortung.android.location.LocationHandler;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.util.MeasurementSystem;

import java.util.Collection;

public class AlertHandler implements OnSharedPreferenceChangeListener {

    public static final AlertCancelEvent ALERT_CANCEL_EVENT = new AlertCancelEvent();
    private final Vibrator vibrator;
    private final NotificationHandler notificationHandler;
    private Context context;
    private Collection<? extends Strike> lastStrikes;
    private int vibrationSignalDuration;
    private Uri alarmSoundNotificationSignal;

    private final AlertParameters alertParameters;

    private Location location;

    private boolean alertEnabled;

    private boolean alarmValid;

    protected Consumer<AlertEvent> alertEventConsumer;

    private final AlertStatus alertStatus;

    private final AlertStatusHandler alertStatusHandler;

    private final LocationHandler locationHandler;

    private float notificationDistanceLimit;

    private long notificationLastTimestamp;

    private float signalingDistanceLimit;

    private long signalingLastTimestamp;

    public AlertHandler(LocationHandler locationHandler, SharedPreferences preferences, Context context, Vibrator vibrator, NotificationHandler notificationHandler, AlertObjectFactory alertObjectFactory, AlertParameters alertParameters) {
        this.locationHandler = locationHandler;
        this.context = context;
        this.vibrator = vibrator;
        this.notificationHandler = notificationHandler;
        this.alertStatus = alertObjectFactory.createAlarmStatus(alertParameters);
        this.alertStatusHandler = alertObjectFactory.createAlarmStatusHandler(alertParameters);
        this.alertParameters = alertParameters;

        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED);
        onSharedPreferenceChanged(preferences, PreferenceKey.MEASUREMENT_UNIT);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_VIBRATION_SIGNAL);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_SOUND_SIGNAL);

        alarmValid = false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case ALERT_ENABLED:
                alertEnabled = sharedPreferences.getBoolean(key.toString(), false);
                break;

            case MEASUREMENT_UNIT:
                String measurementSystemName = sharedPreferences.getString(key.toString(), MeasurementSystem.METRIC.toString());

                alertParameters.setMeasurementSystem(MeasurementSystem.valueOf(measurementSystemName));
                break;

            case ALERT_NOTIFICATION_DISTANCE_LIMIT:
                notificationDistanceLimit = Float.parseFloat(sharedPreferences.getString(key.toString(), "50"));
                break;

            case ALERT_SIGNALING_DISTANCE_LIMIT:
                signalingDistanceLimit = Float.parseFloat(sharedPreferences.getString(key.toString(), "25"));
                break;

            case ALERT_VIBRATION_SIGNAL:
                vibrationSignalDuration = sharedPreferences.getInt(key.toString(), 3) * 10;
                break;

            case ALERT_SOUND_SIGNAL:
                final String signalUri = sharedPreferences.getString(key.toString(), "");
                alarmSoundNotificationSignal = !signalUri.isEmpty() ? Uri.parse(signalUri) : null;
                break;
        }
    }

    private void updateLocationHandler() {
        if (alertEnabled && alertEventConsumer != null) {
            locationHandler.requestUpdates(locationEventConsumer);
        } else {
            locationHandler.removeUpdates(locationEventConsumer);
            location = null;
            broadcastClear();
        }
    }

    private final Consumer<LocationEvent> locationEventConsumer = new Consumer<LocationEvent>() {
        @Override
        public void accept(LocationEvent event) {
            Log.v(Main.LOG_TAG, "AlertHandler received location " + location);
            location = event.getLocation();
            checkStrikes(lastStrikes);
        }
    };

    public Consumer<LocationEvent> getLocationEventConsumer() {
        return locationEventConsumer;
    }

    private final Consumer<DataEvent> dataEventConsumer = event -> {
        if (event instanceof ResultEvent) {
            ResultEvent resultEvent = (ResultEvent) event;
            if (!resultEvent.hasFailed() && resultEvent.containsRealtimeData()) {
                checkStrikes(resultEvent.getStrikes());
            } else {
                invalidateAlert();
            }
        } else if (event instanceof ClearDataEvent) {
            invalidateAlert();
        }
    };

    public Consumer<DataEvent> getDataEventConsumer() {
        return dataEventConsumer;
    }

    public boolean isAlertEnabled() {
        return alertEnabled;
    }

    public void checkStrikes(Collection<? extends Strike> strikes) {
        boolean currentAlarmIsValid = isAlertEnabled() && location != null && strikes != null;
        lastStrikes = strikes;

        if (currentAlarmIsValid) {
            alarmValid = true;
            alertStatusHandler.checkStrikes(alertStatus, strikes, location);
            processResult(getAlarmResult());
        } else {
            invalidateAlert();
        }
    }

    public AlertResult getAlarmResult() {
        return alarmValid ? alertStatusHandler.getCurrentActivity(alertStatus) : null;
    }

    public String getTextMessage(float notificationDistanceLimit) {
        return alertStatusHandler.getTextMessage(alertStatus, notificationDistanceLimit);
    }

    public void setAlertEventConsumer(Consumer<AlertEvent> alertEventConsumer) {
        this.alertEventConsumer = alertEventConsumer;
        updateLocationHandler();
    }

    public void unsetAlertListener() {
        alertEventConsumer = null;
        updateLocationHandler();
    }

    public Collection<AlertSector> getAlarmSectors() {
        return alertStatus.getSectors();
    }

    public AlertParameters getAlertParameters() {
        return alertParameters;
    }

    public AlertStatus getAlertStatus() {
        return alarmValid ? alertStatus : null;
    }

    public float getMaxDistance() {
        final float[] ranges = alertParameters.getRangeSteps();
        return ranges[ranges.length - 1];
    }

    public void invalidateAlert() {
        lastStrikes = null;
        boolean previousAlarmValidState = alarmValid;
        alarmValid = false;

        if (previousAlarmValidState) {
            alertStatus.clearResults();
            broadcastClear();
        }
    }

    private void broadcastClear() {
        if (alertEventConsumer != null) {
            alertEventConsumer.accept(ALERT_CANCEL_EVENT);
        }
    }

    private void broadcastResult(AlertResult alertResult) {
        if (alertEventConsumer != null) {
            alertEventConsumer.accept(new AlertResultEvent(alertStatus, alertResult));
        }
    }

    private void processResult(AlertResult alertResult) {
        if (alertResult != null) {

            alertParameters.updateSectorLabels(context);

            if (alertResult.getClosestStrikeDistance() <= signalingDistanceLimit) {
                long signalingLatestTimestamp = alertStatusHandler.getLatestTimstampWithin(signalingDistanceLimit, alertStatus);
                if (signalingLatestTimestamp > signalingLastTimestamp) {
                    Log.v(Main.LOG_TAG, "AlertHandler.processResult() perform alarm");
                    vibrateIfEnabled();
                    playSoundIfEnabled();
                    signalingLastTimestamp = signalingLatestTimestamp;
                } else {
                    Log.d(Main.LOG_TAG, String.format("old signaling event: %d vs %d", signalingLatestTimestamp, signalingLastTimestamp));
                }
            }

            if (alertResult.getClosestStrikeDistance() <= notificationDistanceLimit) {
                long notificationLatestTimestamp = alertStatusHandler.getLatestTimstampWithin(notificationDistanceLimit, alertStatus);
                if (notificationLatestTimestamp > notificationLastTimestamp) {
                    Log.v(Main.LOG_TAG, "AlertHandler.processResult() perform notification");
                    notificationHandler.sendNotification(context.getResources().getString(R.string.activity) + ": " + getTextMessage(notificationDistanceLimit));
                    notificationLastTimestamp = notificationLatestTimestamp;
                } else {
                    Log.d(Main.LOG_TAG, String.format("AlertHandler.processResult() previous signaling event: %d vs %d", notificationLatestTimestamp, signalingLastTimestamp));
                }
            } else {
                notificationHandler.clearNotification();
            }
        } else {
            notificationHandler.clearNotification();
        }

        Log.v(Main.LOG_TAG, String.format("AlertHandler.processResult() broadcast result %s", alertResult));

        broadcastResult(alertResult);
    }

    private void vibrateIfEnabled() {
        vibrator.vibrate(vibrationSignalDuration);
    }

    private void playSoundIfEnabled() {
        if (alarmSoundNotificationSignal != null) {
            Ringtone r = RingtoneManager.getRingtone(context, alarmSoundNotificationSignal);
            if (r != null) {
                if (!r.isPlaying()) {
                    r.setStreamType(AudioManager.STREAM_NOTIFICATION);
                    r.play();
                }
                Log.v(Main.LOG_TAG, "playing " + r.getTitle(context));
            }
        }
    }

    public Location getCurrentLocation() {
        return location;
    }

    public AlertEvent getAlertEvent() {
        return alarmValid ? new AlertResultEvent(alertStatus, getAlarmResult()) : ALERT_CANCEL_EVENT;
    }

    public void reconfigureLocationHandler() {
        locationHandler.updateProvider();
    }
}
