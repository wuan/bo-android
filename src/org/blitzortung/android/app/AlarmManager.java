package org.blitzortung.android.app;

import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
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

	private TimerTask timerTask;

	private LocationManager locationManager;
	
	Vibrator vibrator;
	
	private Location location;
	
	boolean alarmEnabled;

	public AlarmManager(Main main, SharedPreferences preferences, TimerTask timerTask) {
		this.timerTask = timerTask;

		locationManager = (LocationManager) main.getSystemService(Context.LOCATION_SERVICE);
		vibrator = (Vibrator) main.getSystemService(Context.VIBRATOR_SERVICE);

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
			}
			//Log.v("AlarmManager", "alarm enabled: " + alarmEnabled);
			timerTask.setAlarmEnabled(alarmEnabled);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		//Log.v("AlarmManager", "location changed: " + location);
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
			Raster raster = result.getRaster();
			
			List<AbstractStroke> strokes = result.getStrokes();
			
			int locLon = raster.getLongitudeIndex(location.getLongitude());
			int locLat = raster.getLatitudeIndex(location.getLatitude());
			
			double minDistance = Double.POSITIVE_INFINITY;
			for (AbstractStroke stroke : strokes) {
				int diffLon = raster.getLongitudeIndex(stroke.getLongitude()) - locLon;
				int diffLat = locLat - raster.getLatitudeIndex(stroke.getLatitude());
				
				double distance = Math.sqrt(diffLon * diffLon + diffLat * diffLat);
				minDistance = Math.min(distance, minDistance);
			}
			
			//Log.v("AlarmManager", "minDistance = " + minDistance);
			
			if (minDistance < 4) {
				vibrator.vibrate(40);
			}
		}
	}
}
