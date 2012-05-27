package org.blitzortung.android.app;

import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.data.Provider;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;


public class PersistedData {

	private TimerTask timerTask;
	
	private Provider provider;
	
	private AlarmManager alarmManager;
	
	private StrokesOverlay strokesOverlay;
	
	public PersistedData(OwnMapActivity activity, SharedPreferences preferences) {
		provider = new Provider(preferences);
		timerTask = new TimerTask(activity.getResources(), preferences, provider);
		LocationManager locationService = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
		alarmManager = new AlarmManager(locationService, preferences, timerTask);
		
		strokesOverlay = new StrokesOverlay(activity, new StrokeColorHandler(preferences));
	}

	public StrokesOverlay getStrokesOverlay() {
		return strokesOverlay;
	}

	public TimerTask getTimerTask() {
		return timerTask;
	}

	public Provider getProvider() {
		return provider;
	}

	public AlarmManager getAlarmManager() {
		return alarmManager;
	}
	
}
