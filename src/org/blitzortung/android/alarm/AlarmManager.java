package org.blitzortung.android.alarm;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.app.view.AlarmView;
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

	private final Set<AlarmListener> alarmListeners;

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

			// Log.v("AlarmManager", "alarm enabled: " + alarmEnabled);
			timerTask.setAlarmEnabled(alarmEnabled);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		// Log.v("AlarmManager", "location changed: " + location);
		this.location = location;
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

	public boolean isAlarmEnabled() {
		return true;
	}

	public void check(DataResult result) {

		if (alarmEnabled && location != null) {

			long alarmInterval = 600000;
			long now = new Date().getTime();
			long thresholdTime = now - alarmInterval;

			if (alarmStatus == null || !result.isIncremental()) {
				alarmStatus = new AlarmStatus(thresholdTime);
			} else {
				alarmStatus.updateThresholdTime(thresholdTime);
			}

			List<AbstractStroke> strokes = result.getStrokes();

			for (AbstractStroke stroke : strokes) {
				int multiplicity = stroke.getMultiplicity();

				Location strokeLocation = stroke.getLocation();
				long time = stroke.getTimestamp();

				float distance = location.distanceTo(strokeLocation);
				float bearing = location.bearingTo(strokeLocation);

				alarmStatus.check(multiplicity, distance, bearing, time);
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

	public void removeAlarmListener(AlarmView alarmView) {
		alarmListeners.remove(alarmView);
	}
}
