package org.blitzortung.android.app;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import org.blitzortung.android.data.DataHandler;

public class TimerTask implements Runnable, OnSharedPreferenceChangeListener {

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
		onSharedPreferenceChanged(preferences, Preferences.QUERY_PERIOD_KEY);
		onSharedPreferenceChanged(preferences, Preferences.BACKGROUND_QUERY_PERIOD_KEY);
	}

	@Override
	public void run() {
		long actualSecond = System.currentTimeMillis() / 1000;

		DataHandler.UpdateTargets updateTargets = new DataHandler.UpdateTargets();

		int currentPeriod = backgroundOperation ? backgroundPeriod : period;

		if (actualSecond >= lastUpdate + currentPeriod) {
			updateTargets.addStrokes();
			lastUpdate = actualSecond;
		}

		if (actualSecond >= lastParticipantsUpdate + currentPeriod * 10 && !backgroundOperation) {
			updateTargets.addParticipants();
			lastParticipantsUpdate = actualSecond;
		}

		if (updateTargets.anyUpdateRequested()) {
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

	public void onPause() {
		backgroundOperation = true;
		if (!alarmEnabled || backgroundPeriod == 0) {
			handler.removeCallbacks(this);
		}
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.QUERY_PERIOD_KEY)) {
			period = Integer.parseInt(sharedPreferences.getString(key, "60"));
		} else if (key.equals(Preferences.BACKGROUND_QUERY_PERIOD_KEY)) {
			backgroundPeriod = Integer.parseInt(sharedPreferences.getString(key, "0"));
		}
	}

	public void setAlarmEnabled(boolean alarmEnabled) {
		this.alarmEnabled = alarmEnabled;
	}

	public void setListener(TimerUpdateListener listener) {
		this.listener = listener;
	}

    protected void enable() {
        handler.removeCallbacks(this);
        handler.post(this);
    }

    protected void disable() {
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
