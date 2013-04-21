package org.blitzortung.android.app;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;

import java.util.HashSet;
import java.util.Set;

public class TimerTask implements Runnable, OnSharedPreferenceChangeListener {

    private boolean updateParticipants;
    private boolean enabled;

    public interface TimerUpdateListener {
        public void onTimerUpdate(String timerStatus);
    }

    private int period;

    private int backgroundPeriod;

    private long lastUpdate;

    private long lastParticipantsUpdate;

    private boolean alarmEnabled;

    private boolean backgroundOperation;

    private final DataHandler dataHandler;

    private final Handler handler;

    private TimerUpdateListener listener;

    TimerTask(SharedPreferences preferences, DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        handler = new Handler();
        listener = null;

        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD);
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS);
    }

    @Override
    public void run() {
        long actualSecond = System.currentTimeMillis() / 1000;

        Set<DataChannel> updateTargets = new HashSet<DataChannel>();

        int currentPeriod = backgroundOperation ? backgroundPeriod : period;

        if (actualSecond >= lastUpdate + currentPeriod) {
            updateTargets.add(DataChannel.STROKES);
            lastUpdate = actualSecond;
        }

        if (updateParticipants && actualSecond >= lastParticipantsUpdate + currentPeriod * 10 && !backgroundOperation) {
            updateTargets.add(DataChannel.PARTICIPANTS);
            lastParticipantsUpdate = actualSecond;
        }

        if (!updateTargets.isEmpty()) {
            dataHandler.updateData(updateTargets);
        }

        if (!backgroundOperation && listener != null) {
            listener.onTimerUpdate(String.format("%d/%ds", currentPeriod - (actualSecond - lastUpdate), currentPeriod));
        }

        // Schedule the next update
        handler.postDelayed(this, backgroundOperation ? 60000 : 1000);
    }

    public void restart() {
        lastUpdate = 0;
        lastParticipantsUpdate = 0;
    }

    public void onResume(boolean isRealtime) {
        backgroundOperation = false;
        if (isRealtime) {
            enable();
        }
    }

    public boolean onPause() {
        backgroundOperation = true;
        if (!alarmEnabled || backgroundPeriod == 0) {
            handler.removeCallbacks(this);
            return true;
        }
        return false;
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
        }
    }

    public void setAlarmEnabled(boolean alarmEnabled) {
        this.alarmEnabled = alarmEnabled;
    }

    public void setListener(TimerUpdateListener listener) {
        this.listener = listener;
    }

    public void enable() {
        handler.removeCallbacks(this);
        handler.post(this);
        enabled = true;
    }

    public boolean isEnabled() 
    {
        return enabled;  
    }
    
    protected void disable() {
        enabled = false;
        handler.removeCallbacks(this);
    }

    protected int getPeriod() {
        return period;
    }

    protected int getBackgroundPeriod() {
        return backgroundPeriod;
    }

    protected long getLastUpdate() {
        return lastUpdate;
    }

    protected long getLastParticipantsUpdate() {
        return lastParticipantsUpdate;
    }

    protected boolean isInBackgroundOperation() {
        return backgroundOperation;
    }

    protected boolean getAlarmEnabled() {
        return alarmEnabled;
    }
}
