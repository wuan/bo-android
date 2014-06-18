package org.blitzortung.android.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.event.AlertEvent;
import org.blitzortung.android.alert.AlertHandler;
import org.blitzortung.android.alert.factory.AlertObjectFactory;
import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.provider.result.ClearDataEvent;
import org.blitzortung.android.data.provider.result.ResultEvent;
import org.blitzortung.android.location.LocationEvent;
import org.blitzortung.android.location.LocationHandler;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.provider.result.DataEvent;
import org.blitzortung.android.data.provider.result.StatusEvent;
import org.blitzortung.android.protocol.Consumer;
import org.blitzortung.android.protocol.ConsumerContainer;
import org.blitzortung.android.util.Period;

import java.util.HashSet;
import java.util.Set;

public class AppService extends Service implements Runnable, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String RETRIEVE_DATA_ACTION = "retrieveData";
    public static final String WAKE_LOCK_TAG = "boAndroidWakeLock";

    private final Handler handler;

    private int period;

    private int backgroundPeriod;

    private final Period updatePeriod;

    private Parameters lastParameters;

    private boolean updateParticipants;

    private boolean enabled;

    private DataHandler dataHandler;
    private AlertHandler alertHandler;
    private boolean alertEnabled;
    private LocationHandler locationHandler;

    private final IBinder binder = new DataServiceBinder();

    private AlarmManager alarmManager;

    private PendingIntent pendingIntent;

    private PowerManager.WakeLock wakeLock;

    ConsumerContainer<DataEvent> dataConsumerContainer = new ConsumerContainer<DataEvent>() {
        @Override
        public void addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "added first data consumer");
            configureServiceMode();
        }

        @Override
        public void removedLastConsumer() {
            Log.d(Main.LOG_TAG, "removed last data consumer");
            configureServiceMode();
        }
    };

    ConsumerContainer<AlertEvent> alertConsumerContainer = new ConsumerContainer<AlertEvent>() {
        @Override
        public void addedFirstConsumer() {
            Log.d(Main.LOG_TAG, "added first alert consumer");
        }

        @Override
        public void removedLastConsumer() {
            Log.d(Main.LOG_TAG, "removed last alert consumer");
        }
    };

    @SuppressWarnings("UnusedDeclaration")
    public AppService() {
        this(new Handler(), new Period());
        Log.d(Main.LOG_TAG, "AppService() created with new handler");
    }

    protected AppService(Handler handler, Period updatePeriod) {
        Log.d(Main.LOG_TAG, "AppService() create");
        this.handler = handler;
        this.updatePeriod = updatePeriod;
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

    public AlertHandler getAlertHandler() {
        return alertHandler;
    }

    private final Consumer<DataEvent> dataEventConsumer = new Consumer<DataEvent>() {
        @Override
        public void consume(DataEvent event) {
            if (!dataConsumerContainer.isEmpty()) {
                dataConsumerContainer.storeAndBroadcast(event);
            }

            if (alertEnabled) {
                alertHandler.getDataEventConsumer().consume(event);
            }

            if (event instanceof ClearDataEvent) {
                restart();
            } else if (event instanceof ResultEvent) {
                ResultEvent resultEvent = (ResultEvent) event;
                lastParameters = resultEvent.getParameters();
                configureServiceMode();
            }

            releaseWakeLock();
        }
    };

    private final Consumer<AlertEvent> alertEventConsumer = new Consumer<AlertEvent>() {
        @Override
        public void consume(AlertEvent event) {
            alertConsumerContainer.storeAndBroadcast(event);
        }
    };

    public void addDataConsumer(Consumer<DataEvent> dataConsumer) {
        dataConsumerContainer.addConsumer(dataConsumer);
    }

    public void removeDataConsumer(Consumer<DataEvent> dataConsumer) {
        dataConsumerContainer.removeConsumer(dataConsumer);
    }

    public void addAlertConsumer(Consumer<AlertEvent> alertConsumer) {
        alertConsumerContainer.addConsumer(alertConsumer);
    }

    public void removeAlertListener(Consumer<AlertEvent> alertConsumer) {
        alertConsumerContainer.removeConsumer(alertConsumer);
    }

    public void removeLocationConsumer(Consumer<LocationEvent> locationConsumer) {
        locationHandler.removeUpdates(locationConsumer);
    }

    public void addLocationConsumer(Consumer<LocationEvent> locationListener) {
        locationHandler.requestUpdates(locationListener);
    }

    public AlertEvent getAlertEvent() {
        return alertHandler.getAlertEvent();
    }

    public class DataServiceBinder extends Binder {
        AppService getService() {
            Log.d(Main.LOG_TAG, "DataServiceBinder.getService() " + AppService.this);
            return AppService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(Main.LOG_TAG, "AppService.onCreate()");
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (wakeLock == null) {
            Log.d(Main.LOG_TAG, "AppService.onCreate() create wakelock");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        }

        if (dataHandler == null) {
            dataHandler = new DataHandler(wakeLock, preferences, getPackageInfo());
            dataHandler.setDataConsumer(dataEventConsumer);
        }

        locationHandler = new LocationHandler(this, preferences);
        AlertParameters alertParameters = new AlertParameters();
        alertParameters.updateSectorLabels(this);
        alertHandler = new AlertHandler(locationHandler, preferences, this,
                (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE),
                new NotificationHandler(this),
                new AlertObjectFactory(), alertParameters);

        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED);
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: " + startId + " " + intent);

        if (intent != null && RETRIEVE_DATA_ACTION.equals(intent.getAction())) {
            acquireWakeLock();

            Log.v(Main.LOG_TAG, "AppService.onStartCommand() acquired wake lock " + wakeLock);

            enabled = false;
            handler.removeCallbacks(this);
            handler.post(this);
        }

        return START_STICKY;
    }

    private void acquireWakeLock() {
        wakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (wakeLock.isHeld()) {
            try {
                wakeLock.release();
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() " + wakeLock);
            } catch (RuntimeException e) {
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() failed", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(Main.LOG_TAG, "AppService.onBind() " + intent);

        return binder;
    }

    @Override
    public void run() {
        if (dataConsumerContainer.isEmpty()) {
            if (alertEnabled && backgroundPeriod > 0) {
                Log.v(Main.LOG_TAG, "AppService.run() in background");

                dataHandler.updateDatainBackground();
            } else {
                enabled = false;
                handler.removeCallbacks(this);
            }
        } else {
            releaseWakeLock();

            long currentTime = Period.getCurrentTime();
            if (dataHandler != null) {
                Set<DataChannel> updateTargets = new HashSet<DataChannel>();

                if (updatePeriod.shouldUpdate(currentTime, period)) {
                    updatePeriod.setLastUpdateTime(currentTime);
                    updateTargets.add(DataChannel.STROKES);

                    if (updateParticipants && updatePeriod.isNthUpdate(10)) {
                        updateTargets.add(DataChannel.PARTICIPANTS);
                    }
                }

                if (!updateTargets.isEmpty()) {
                    dataHandler.updateData(updateTargets);
                }

                final String statusString = "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period;
                dataConsumerContainer.broadcast(new StatusEvent(statusString));
            }
            // Schedule the next update
            handler.postDelayed(this, 1000);
        }
    }

    public void restart() {
        configureServiceMode();
        updatePeriod.restart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(Main.LOG_TAG, "AppService.onDestroy()");
    }

    public boolean isEnabled() {
        return enabled;
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
            case ALERT_ENABLED:
                alertEnabled = sharedPreferences.getBoolean(key.toString(), false);

                configureServiceMode();
                break;

            case QUERY_PERIOD:
                period = Integer.parseInt(sharedPreferences.getString(key.toString(), "60"));
                break;

            case BACKGROUND_QUERY_PERIOD:
                backgroundPeriod = Integer.parseInt(sharedPreferences.getString(key.toString(), "0"));

                Log.v(Main.LOG_TAG, String.format("AppService.onSharedPreferenceChanged() backgroundPeriod=%d", backgroundPeriod));
                discardAlarm();
                configureServiceMode();
                break;

            case SHOW_PARTICIPANTS:
                updateParticipants = sharedPreferences.getBoolean(key.toString(), true);
                break;
        }
    }

    private void configureServiceMode() {
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() entered");
        final boolean backgroundOperation = dataConsumerContainer.isEmpty();
        if (backgroundOperation) {
            if (alertEnabled && backgroundPeriod > 0) {
                locationHandler.enableBackgroundMode();
                alertHandler.setAlertEventConsumer(alertEventConsumer);
                alertHandler.reconfigureLocationHandler();
                createAlarm();
            } else {
                alertHandler.unsetAlertListener();
                discardAlarm();
            }
        } else {
            discardAlarm();
            if (dataHandler.isRealtime()) {
                Log.v(Main.LOG_TAG, "AppService.configureServiceMode() realtime data");
                if (!enabled) {
                    enabled = true;
                    handler.removeCallbacks(this);
                    handler.post(this);
                }
            } else {
                Log.v(Main.LOG_TAG, "AppService.configureServiceMode() historic data");
                enabled = false;
                handler.removeCallbacks(this);
                if (lastParameters != null && !lastParameters.equals(dataHandler.getParameters())) {
                    dataHandler.updateData();
                }
            }
            locationHandler.disableBackgroundMode();
            Log.v(Main.LOG_TAG, "AppService.configureServiceMode() set alert event consumer");
            alertHandler.setAlertEventConsumer(alertEventConsumer);
            alertHandler.reconfigureLocationHandler();
        }
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() done");
    }

    private void createAlarm() {
        if (alarmManager == null && dataConsumerContainer.isEmpty() && backgroundPeriod > 0) {
            Log.v(Main.LOG_TAG, String.format("AppService.createAlarm() with backgroundPeriod=%d", backgroundPeriod));
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
    }

    private void discardAlarm() {
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
