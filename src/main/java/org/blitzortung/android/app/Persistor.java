package org.blitzortung.android.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;
import org.blitzortung.android.app.controller.LocationHandler;
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

    public LocationHandler getLocationHandler() {
        return locationHandler;
    }

    private final LocationHandler locationHandler;

    public Persistor(Context context, SharedPreferences sharedPreferences, PackageInfo pInfo) {
        provider = new DataHandler(sharedPreferences, pInfo);
		timerTask = new TimerTask(sharedPreferences, provider);
        locationHandler = new LocationHandler(context, sharedPreferences);
		alarmManager = new AlarmManager(locationHandler, sharedPreferences, new AlarmObjectFactory(), new AlarmParameters());
		strokesOverlay = new StrokesOverlay(context, new StrokeColorHandler(sharedPreferences));
		participantsOverlay = new ParticipantsOverlay(context, new ParticipantColorHandler(sharedPreferences));
	}

    public void updateContext(Main context)
    {
        strokesOverlay.setActivity(context);
        participantsOverlay.setActivity(context);
        provider.setDataListener(context);
        timerTask.setListener(context);

        alarmManager.clearAlarmListeners();
        alarmManager.addAlarmListener(context);
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
