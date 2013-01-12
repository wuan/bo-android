package org.blitzortung.android.app;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.location.LocationManager;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

public class Persistor {

	private final TimerTask timerTask;

	private final DataHandler provider;
	
	private final AlarmManager alarmManager;
	
	private final StrokesOverlay strokesOverlay;
	
	private final ParticipantsOverlay participantsOverlay;

    private DataResult currentResult;

    public Persistor(LocationManager locationManager, SharedPreferences sharedPreferences, PackageInfo pInfo) {
        provider = new DataHandler(sharedPreferences, pInfo);
		timerTask = new TimerTask(sharedPreferences, provider);
		alarmManager = new AlarmManager(locationManager, sharedPreferences, timerTask);
		strokesOverlay = new StrokesOverlay(new StrokeColorHandler(sharedPreferences));
		participantsOverlay = new ParticipantsOverlay(new ParticipantColorHandler(sharedPreferences));
	}

	public StrokesOverlay getStrokesOverlay() {
		return strokesOverlay;
	}
	
	public ParticipantsOverlay getParticipantsOverlay() {
		return participantsOverlay;
	}

	public TimerTask getTimerTask() {
		return timerTask;
	}

	public DataHandler getDataHandler() {
		return provider;
	}

	public AlarmManager getAlarmManager() {
		return alarmManager;
	}

    public void setCurrentResult(DataResult result) {
        currentResult = result;
    }

    public DataResult getCurrentResult() {
        return currentResult;
    }

    public boolean hasCurrentResult() {
        return  currentResult != null;
    }
}
