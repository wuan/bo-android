package org.blitzortung.android.app;

import java.util.Calendar;

import org.blitzortung.android.data.DataRetriever;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Handler;

public class TimerTask implements Runnable, OnSharedPreferenceChangeListener {

	public interface StatusListener {
		public void onStatusUpdate(String statusString);
	}

	private int period;

	private int backgroundPeriod;

	private long lastUpdate;

	private long lastParticipantsUpdate;

	private int numberOfStrokes;

	private boolean alarmEnabled;

	private boolean backgroundOperation;

	private final DataRetriever provider;

	private final Handler handler;

	StatusListener listener;

	final Resources resources;

	TimerTask(Resources resources, SharedPreferences preferences, DataRetriever provider) {
		this.resources = resources;
		this.provider = provider;
		handler = new Handler();
		listener = null;

		preferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(preferences, Preferences.QUERY_PERIOD_KEY);
		onSharedPreferenceChanged(preferences, Preferences.BACKGROUND_QUERY_PERIOD_KEY);
		onSharedPreferenceChanged(preferences, Preferences.ALARM_ENABLED_KEY);
	}

	@Override
	public void run() {
		long now = Calendar.getInstance().getTimeInMillis() / 1000;

		DataRetriever.UpdateTargets updateTargets = new DataRetriever.UpdateTargets();

		int currentPeriod = backgroundOperation ? backgroundPeriod : period;

		if (now >= lastUpdate + currentPeriod) {
			updateTargets.addStrokes();
			lastUpdate = now;
		}
		
		if (now >= lastParticipantsUpdate + currentPeriod * 10 && !backgroundOperation) {
			updateTargets.addParticipants();
			lastParticipantsUpdate = now;
		}

		if (updateTargets.anyUpdateRequested()) {
			provider.updateData(updateTargets);
		}

		if (!backgroundOperation && listener != null) {
			String statusString = resources.getQuantityString(R.plurals.stroke, numberOfStrokes, numberOfStrokes);
			statusString += "/";
			statusString += resources.getQuantityString(R.plurals.minute, provider.getMinutes(), provider.getMinutes());
			statusString += String.format(" %d/%ds", currentPeriod - (now - lastUpdate), currentPeriod);

			if (provider.isUsingRaster()) {
				int region = provider.getRegion();
				String regions[] = resources.getStringArray(R.array.regions_values);
				int index = 0;
				for (String region_number : regions) {
					if (region == Integer.parseInt(region_number)) {
						statusString += " " + resources.getStringArray(R.array.regions)[index];
						break;
					}
					index++;
				}
			}

			listener.onStatusUpdate(statusString);
		}

		// Schedule the next update
		handler.postDelayed(this, backgroundOperation ? 60000 : 1000);
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public void restart() {
		lastUpdate = 0;
		lastParticipantsUpdate = 0;
	}

	public void onResume() {
		backgroundOperation = false;
		handler.removeCallbacks(this);
		handler.post(this);
	}

	public void onPause() {
		backgroundOperation = true;
		if (!alarmEnabled || backgroundPeriod == 0) {
			handler.removeCallbacks(this);
		}
	}

	public void setNumberOfStrokes(int numberOfStrokes) {
		this.numberOfStrokes = numberOfStrokes;
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

	public void setListener(StatusListener listener) {
		this.listener = listener;
	}

}
