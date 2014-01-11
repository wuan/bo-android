package org.blitzortung.android.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Vibrator;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.component.ParticipantsComponent;
import org.blitzortung.android.data.component.StrokesComponent;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.map.color.StrokeColorHandler;

public class Persistor {

    private final DataHandler provider;

    private final AlarmManager alarmManager;
    private final StrokesComponent strokesComponent;
    private final ParticipantsComponent participantsComponent;

    private DataResult currentResult;

    public LocationHandler getLocationHandler() {
        return locationHandler;
    }

    private final LocationHandler locationHandler;

    public Persistor(Activity activity, SharedPreferences sharedPreferences, PackageInfo pInfo) {
        provider = new DataHandler(sharedPreferences, pInfo);
        locationHandler = new LocationHandler(activity, sharedPreferences);
        AlarmParameters alarmParameters = new AlarmParameters();
        alarmParameters.updateSectorLabels(activity);
        alarmManager = new AlarmManager(locationHandler, sharedPreferences, activity, (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE), new NotificationHandler(activity), new AlarmObjectFactory(), alarmParameters);
        strokesComponent = new StrokesComponent();
        strokesComponent.setColorHandler(new StrokeColorHandler(sharedPreferences));
        participantsComponent = new ParticipantsComponent();
    }

    public void updateContext(Main mainActivity) {
        provider.setDataListener(mainActivity);

        alarmManager.updateContext(mainActivity);
        alarmManager.clearAlarmListeners();
        alarmManager.addAlarmListener(mainActivity);
    }

    public StrokesComponent getStrokesComponent() {
        return strokesComponent;
    }

    public ParticipantsComponent getParticipantsComponent() {
        return participantsComponent;
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
        return currentResult != null;
    }
}
