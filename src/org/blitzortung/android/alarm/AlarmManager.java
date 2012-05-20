package org.blitzortung.android.alarm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.provider.DataResult;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;

public class AlarmManager implements OnSharedPreferenceChangeListener, LocationListener {

	public interface AlarmListener {
		void onAlarmResult(AlarmStatus alarmStatus);

		void onAlarmClear();
	}

	private TimerTask timerTask;

	private LocationManager locationManager;

	Vibrator vibrator;

	private Location location;

	boolean alarmEnabled;

	Set<AlarmListener> alarmListeners;

	AlarmStatus alarmStatus;

	public AlarmManager(Main main, SharedPreferences preferences, TimerTask timerTask) {
		this.timerTask = timerTask;

		alarmListeners = new HashSet<AlarmListener>();

		locationManager = (LocationManager) main.getSystemService(Context.LOCATION_SERVICE);

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
			
			alarmStatus = new AlarmStatus();

			List<AbstractStroke> strokes = result.getStrokes();

			for (AbstractStroke stroke : strokes) {
				int multiplicity = stroke.getCount();

				Location strokeLocation = stroke.getLocation();
				long time = stroke.getTime().getTime();

				float distance = location.distanceTo(strokeLocation);
				float bearing = location.bearingTo(strokeLocation);

				alarmStatus.check(multiplicity, distance, bearing, time);
			}
			
			for (AlarmListener alarmListener: alarmListeners) {
				alarmListener.onAlarmResult(alarmStatus);
			}
		}
	}

	public AlarmStatus getAlarmStatus() {
		return alarmStatus;
	}

	public void addAlarmListener(AlarmListener alarmListener) {
		alarmListeners.add(alarmListener);
	}

	public void removeAlarmListener(AlarmView alarmView) {
		alarmListeners.remove(alarmView);
	}
}
