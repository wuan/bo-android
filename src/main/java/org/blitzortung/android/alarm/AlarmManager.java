package org.blitzortung.android.alarm;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.provider.DataResult;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class AlarmManager implements OnSharedPreferenceChangeListener, LocationListener {

	public interface AlarmListener {
		void onAlarmResult(AlarmStatus alarmStatus);

		void onAlarmClear();
	}

	private final TimerTask timerTask;

	private final LocationManager locationManager;

	private Location location;

	private boolean alarmEnabled;

    // VisibleForTesting
	protected final Set<AlarmListener> alarmListeners;

	private AlarmStatus alarmStatus;

	public AlarmManager(LocationManager locationManager, SharedPreferences preferences, TimerTask timerTask) {
		this.timerTask = timerTask;

		alarmListeners = new HashSet<AlarmListener>();

		this.locationManager = locationManager;

		preferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(preferences, Preferences.ALARM_ENABLED_KEY);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.ALARM_ENABLED_KEY)) {
			alarmEnabled = sharedPreferences.getBoolean(key, false);

			if (alarmEnabled) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
			} else {
				locationManager.removeUpdates(this);

				for (AlarmListener alarmListener : alarmListeners) {
					alarmListener.onAlarmClear();
				}
			}

			timerTask.setAlarmEnabled(alarmEnabled);
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

		if (alarmEnabled && location != null) {

			long alarmInterval = 600000;
			long now = new Date().getTime();
			long thresholdTime = now - alarmInterval;

			if (alarmStatus == null || !result.isIncremental()) {
				alarmStatus = new AlarmStatus(thresholdTime);
			} else {
				alarmStatus.updateWarnThresholdTime(thresholdTime);
			}

			for (AbstractStroke stroke : result.getStrokes()) {
				alarmStatus.check(location, stroke);
			}
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
