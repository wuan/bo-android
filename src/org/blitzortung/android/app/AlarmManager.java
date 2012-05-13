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

	interface AlarmListener {
		void onAlarmResult(double distance, double bearing);

		void onAlarmClear();
	}

	private TimerTask timerTask;

	private LocationManager locationManager;

	Vibrator vibrator;

	private Location location;

	boolean alarmEnabled;

	AlarmListener alarmListener;

	public AlarmManager(Main main, SharedPreferences preferences, TimerTask timerTask) {
		this.timerTask = timerTask;
		alarmListener = null;

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
				if (alarmListener != null) {
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

	public void setAlarmListener(AlarmListener alarmListener) {
		this.alarmListener = alarmListener;
	}

	public void check(DataResult result) {
		if (alarmEnabled && location != null) {
			Raster raster = result.getRaster();

			List<AbstractStroke> strokes = result.getStrokes();

			int locLon = raster.getLongitudeIndex(location.getLongitude());
			int locLat = raster.getLatitudeIndex(location.getLatitude());

			double minDistance = Double.POSITIVE_INFINITY;
			int minDistanceIndex = -1;

			int index = 0;
			for (AbstractStroke stroke : strokes) {
				int diffLon = raster.getLongitudeIndex(stroke.getLongitude()) - locLon;
				int diffLat = locLat - raster.getLatitudeIndex(stroke.getLatitude());

				double distance = Math.sqrt(diffLon * diffLon + diffLat * diffLat);
				minDistance = Math.min(distance, minDistance);
				if (distance == minDistance) {
					minDistanceIndex = index;
				}
				index++;
			}

			double distance = -1.0;
			double bearing = 0.0;

			if (minDistanceIndex >= 0) {
				Location closestStrokeLocation = new Location("");
				AbstractStroke closestStroke = strokes.get(minDistanceIndex);
				closestStrokeLocation.setLongitude(closestStroke.getLongitude());
				closestStrokeLocation.setLatitude(closestStroke.getLatitude());

				distance = location.distanceTo(closestStrokeLocation);
				bearing = location.bearingTo(closestStrokeLocation);
			}

			if (alarmListener != null) {
				alarmListener.onAlarmResult(distance, bearing);
			}
		}
	}
}
