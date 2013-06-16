package org.blitzortung.android.app;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.util.Period;

import java.util.HashSet;
import java.util.Set;

public class DataService extends Service implements Runnable, SharedPreferences.OnSharedPreferenceChangeListener {

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

    @SuppressWarnings("UnusedDeclaration")
    public DataService() {
        this(new Handler(), new Period());
        Log.d(Main.LOG_TAG, "DataService() created with new handler");
    }

    protected DataService(Handler handler, Period updatePeriod) {
        Log.d(Main.LOG_TAG, "DataService() create");
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

    public class DataServiceBinder extends Binder {
        DataService getService() {
            Log.d(Main.LOG_TAG, "DataServiceBinder.getService() " + DataService.this);
            return DataService.this;
        }
    }

    public interface DataServiceStatusListener {
        public void onDataServiceStatusUpdate(String dataServiceStatus);
    }

    @Override
    public void onCreate() {
        Log.i(Main.LOG_TAG, "DataService.onCreate()");
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.ALARM_ENABLED);
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Main.LOG_TAG, "DataService.onStartCommand() Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(Main.LOG_TAG, "DataService.onBind() " + intent);
        return binder;
    }

    @Override
    public void run() {
        long currentTime = Period.getCurrentTime();
        
        if (backgroundOperation) {
            Log.v(Main.LOG_TAG, "DataService: run in background");
        }

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
        handler.postDelayed(this, getCurrentPostDelay());
    }

    private int getCurrentPeriod() {
        return backgroundOperation ? backgroundPeriod : period;
    }

    private int getCurrentPostDelay() {
        return backgroundOperation ? 60000 : 1000;
    }

    public void restart() {
        updatePeriod.restart();
    }

    public void onResume() {
        backgroundOperation = false;
        if (dataHandler.isRealtime()) {
            Log.v(Main.LOG_TAG, "DataService: onResume() enable");
            enable();
        } else {
            Log.v(Main.LOG_TAG, "DataService: onResume() do not enable");
        }
    }

    public boolean onPause() {
        backgroundOperation = true;
        if (!alarmEnabled || backgroundPeriod == 0) {
            handler.removeCallbacks(this);
            Log.v(Main.LOG_TAG, "DataService: onPause() remove callback");
            return true;
        }
        Log.v(Main.LOG_TAG, "DataService: onPause() keep callback");
        return false;
    }


    public void setListener(DataServiceStatusListener listener) {
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
                backgroundPeriod = Integer.parseInt(sharedPreferences.getString(key.toString(), "0"));
                break;

            case SHOW_PARTICIPANTS:
                updateParticipants = sharedPreferences.getBoolean(key.toString(), true);
                break;

            case ALARM_ENABLED:
                alarmEnabled = sharedPreferences.getBoolean(key.toString(), false);
                break;
        }
    }


}
