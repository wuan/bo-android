package org.blitzortung.android.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.alarm.AlertHandler;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;
import org.blitzortung.android.alarm.listener.AlertListener;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.util.Period;

import java.util.HashSet;
import java.util.Set;

public class AppService extends Service implements Runnable, SharedPreferences.OnSharedPreferenceChangeListener, DataListener, LocationHandler.Listener {

    public static final String RETRIEVE_DATA_ACTION = "retrieveData";

    public static final String WAKE_LOCK_TAG = "boAndroidWakeLock";

    private final Handler handler;

    private int period;

    private int backgroundPeriod;

    private final Period updatePeriod;

    private boolean alarmEnabled;

    private boolean backgroundOperation;

    private boolean updateParticipants;

    private boolean enabled;

    private DataHandler dataHandler;

    private DataServiceStatusListener listener;

    private final IBinder binder = new DataServiceBinder();

    private AlarmManager alarmManager;

    private PendingIntent pendingIntent;

    private PowerManager.WakeLock wakeLock;
    private LocationHandler locationHandler;
    private AlertHandler alertHandler;
    private DataListener dataListener;
    private LocationHandler.Listener locationListener;

    @SuppressWarnings("UnusedDeclaration")
    public AppService() {
        this(new Handler(), new Period());
        Log.d(Main.LOG_TAG, "AppService() created with new handler");
    }

    protected AppService(Handler handler, Period updatePeriod) {
        Log.d(Main.LOG_TAG, "AppService() create");
        this.handler = handler;
        this.updatePeriod = updatePeriod;
        backgroundOperation = true;
    }

    public int getPeriod() {
        return period;
    }

    public int getBackgroundPeriod() {
        return backgroundPeriod;
    }

    public long getLastUpdate() {
        return updatePeriod.getLastUpdateTime();
    }

    public boolean isInBackgroundOperation() {
        return backgroundOperation;
    }

    public void reloadData() {
        if (isEnabled()) {
            restart();
        } else {
            Set<DataChannel> updateTargets = new HashSet<DataChannel>();
            updateTargets.add(DataChannel.STROKES);
            dataHandler.updateData(updateTargets);
        }
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public boolean isAlarmEnabled() {
        return alertHandler.isAlarmEnabled();
    }

    public AlarmResult getAlarmResult() {
        return alertHandler.getAlarmResult();
    }

    public AlertHandler getAlertHandler() {
        return alertHandler;
    }

    @Override
    public void onBeforeDataUpdate() {
        if (dataListener != null) {
            dataListener.onBeforeDataUpdate();
        }
    }

    @Override
    public void onDataUpdate(DataResult result) {
        if (!result.isBackground() && dataListener != null) {
            dataListener.onDataUpdate(result);
        }

        if (result.containsRealtimeData()) {
            alertHandler.checkStrokes(result.getStrokes());
        } else {
            alertHandler.cancelAlert();
        }

        releaseWakeLock();
    }

    @Override
    public void onDataReset() {
        if (dataListener != null) {
            dataListener.onDataReset();
        }
        releaseWakeLock();
    }

    @Override
    public void onDataError() {
        if (dataListener != null) {
            dataListener.onDataError();
        }
        releaseWakeLock();
    }

    public void setDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
    }

    public void setAlertListener(AlertListener alertListener) {
        alertHandler.setAlertListener(alertListener);
    }

    public AlarmStatus getAlarmStatus() {
        return alertHandler.getAlarmStatus();
    }

    public void clearLocationListener() {
        locationListener = null;
    }

    public void setLocationListener(LocationHandler.Listener locationListener) {
        this.locationListener = locationListener;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (locationListener != null) {
            locationListener.onLocationChanged(location);
        }
    }

    public class DataServiceBinder extends Binder {
        AppService getService() {
            Log.d(Main.LOG_TAG, "DataServiceBinder.getService() " + AppService.this);
            return AppService.this;
        }
    }

    public interface DataServiceStatusListener {
        public void onDataServiceStatusUpdate(String dataServiceStatus);
    }

    @Override
    public void onCreate() {
        Log.i(Main.LOG_TAG, "AppService.onCreate()");
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALARM_ENABLED);
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS);

        backgroundOperation = true;

        if (dataHandler == null) {
            dataHandler = new DataHandler(preferences, getPackageInfo());
            dataHandler.setDataListener(this);
        }

        locationHandler = new LocationHandler(this, preferences);
        locationHandler.requestUpdates(this);
        AlarmParameters alarmParameters = new AlarmParameters();
        alarmParameters.updateSectorLabels(this);
        alertHandler = new AlertHandler(locationHandler, preferences, this,
                (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE),
                new NotificationHandler(this),
                new AlarmObjectFactory(), alarmParameters);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: " + startId + " " + intent);

        if (intent != null && RETRIEVE_DATA_ACTION.equals(intent.getAction())) {
            if (backgroundOperation) {
                releaseWakeLock();
                Log.i(Main.LOG_TAG, "AppService.onStartCommand() wakeLock released");

                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
                wakeLock.acquire(20000);

                Log.v(Main.LOG_TAG, "AppService.onStartCommand() acquire wake lock " + wakeLock);

                handler.removeCallbacks(this);
                handler.post(this);
            }
        } else {
            if (backgroundOperation) {
                if (alarmManager == null && backgroundPeriod > 0) {
                    createAlarm();
                }
            } else {
                discardAlarm();
            }
        }

        return START_STICKY;
    }

    public void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() " + wakeLock);
            try {
                wakeLock.release();
            } catch (RuntimeException e) {
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() failed: " + e.toString());
            }
        }
        wakeLock = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(Main.LOG_TAG, "AppService.onBind() " + intent);

        backgroundOperation = false;

        return binder;
    }

    @Override
    public void run() {

        if (backgroundOperation) {
            Log.v(Main.LOG_TAG, "AppService.run() in background");

            dataHandler.updateDatainBackground();
        } else {
            long currentTime = Period.getCurrentTime();
            if (dataHandler != null) {
                Set<DataChannel> updateTargets = new HashSet<DataChannel>();

                int currentPeriod = getCurrentPeriod();

                if (updatePeriod.shouldUpdate(currentTime, currentPeriod)) {
                    updatePeriod.setLastUpdateTime(currentTime);
                    updateTargets.add(DataChannel.STROKES);

                    if (!backgroundOperation && updateParticipants && updatePeriod.isNthUpdate(10)) {
                        updateTargets.add(DataChannel.PARTICIPANTS);
                    }
                }

                if (!updateTargets.isEmpty()) {
                    dataHandler.updateData(updateTargets);
                }

                if (!backgroundOperation && listener != null) {
                    listener.onDataServiceStatusUpdate(String.format("%d/%ds", updatePeriod.getCurrentUpdatePeriod(currentTime, currentPeriod), currentPeriod));
                }
            }
            // Schedule the next update
            handler.postDelayed(this, 1000);
        }
    }

    private int getCurrentPeriod() {
        return backgroundOperation ? backgroundPeriod : period;
    }

    public void restart() {
        updatePeriod.restart();
    }

    public void onResume() {
        backgroundOperation = false;

        discardAlarm();

        if (dataHandler.isRealtime()) {
            Log.v(Main.LOG_TAG, "AppService.onResume() enable");
            enable();
        } else {
            Log.v(Main.LOG_TAG, "AppService.onResume() do not enable");
        }
    }

    public boolean onPause() {
        backgroundOperation = true;

        handler.removeCallbacks(this);
        Log.v(Main.LOG_TAG, "AppService.onPause() remove callback");

        if (alarmEnabled && backgroundPeriod > 0) {
            createAlarm();
            return false;
        }
        return true;
    }

    public void setStatusListener(DataServiceStatusListener listener) {
        this.listener = listener;
    }

    public void enable() {
        handler.removeCallbacks(this);
        handler.post(this);
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected void disable() {
        enabled = false;
        handler.removeCallbacks(this);
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case QUERY_PERIOD:
                period = Integer.parseInt(sharedPreferences.getString(key.toString(), "60"));
                break;

            case BACKGROUND_QUERY_PERIOD:
                int newBackgroundPeriod = Integer.parseInt(sharedPreferences.getString(key.toString(), "0"));

                if (backgroundOperation) {
                    if (backgroundPeriod == 0 && newBackgroundPeriod > 0) {
                        Log.v(Main.LOG_TAG, String.format("AppService.onSharedPreferenceChanged() create alarm with backgroundPeriod=%d", newBackgroundPeriod));
                        createAlarm();
                    } else if (backgroundPeriod > 0 && newBackgroundPeriod == 0) {
                        discardAlarm();
                        Log.v(Main.LOG_TAG, String.format("AppService.onSharedPreferenceChanged() discard alarm", newBackgroundPeriod));
                    }
                } else {
                    Log.v(Main.LOG_TAG, String.format("AppService.onSharedPreferenceChanged() backgroundPeriod=%d", newBackgroundPeriod));
                }
                backgroundPeriod = newBackgroundPeriod;
                break;

            case SHOW_PARTICIPANTS:
                updateParticipants = sharedPreferences.getBoolean(key.toString(), true);
                break;

            case ALARM_ENABLED:
                alarmEnabled = sharedPreferences.getBoolean(key.toString(), false);
                break;
        }
    }

    private void createAlarm() {
        discardAlarm();

        Log.v(Main.LOG_TAG, String.format("AppService.createAlarm() %d", backgroundPeriod));
        Intent intent = new Intent(this, AppService.class);
        intent.setAction(RETRIEVE_DATA_ACTION);
        pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, backgroundPeriod * 1000, pendingIntent);
        } else {
            Log.e(Main.LOG_TAG, "AppService.createAlarm() failed");
        }
    }

    private void discardAlarm() {
        releaseWakeLock();

        if (alarmManager != null) {
            Log.v(Main.LOG_TAG, "AppService.discardAlarm()");
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();

            pendingIntent = null;
            alarmManager = null;
        }
    }

    private PackageInfo getPackageInfo() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
