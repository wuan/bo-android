package org.blitzortung.android.app;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.*;
import com.google.android.maps.Overlay;
import org.blitzortung.android.alarm.AlarmLabel;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmStatus;
import org.blitzortung.android.app.controller.ButtonColumnHandler;
import org.blitzortung.android.app.controller.HistoryController;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.app.view.HistogramView;
import org.blitzortung.android.app.view.LegendView;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.dialogs.AlarmDialog;
import org.blitzortung.android.dialogs.InfoDialog;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.FadeOverlay;
import org.blitzortung.android.map.overlay.OwnLocationOverlay;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;

import java.util.*;

public class Main extends OwnMapActivity implements DataListener, OnSharedPreferenceChangeListener, TimerTask.TimerUpdateListener,
        AlarmManager.AlarmListener {

    protected TextView status;

    private TextView warning;

    private ProgressBar progressBar;

    private ImageView errorIndicator;

    private FadeOverlay fadeOverlay;

    protected StrokesOverlay strokesOverlay;

    private ParticipantsOverlay participantsOverlay;

    private TimerTask timerTask;

    private AlarmManager alarmManager;

    private NotificationHandler notificationHandler;

    private DataHandler dataHandler;

    private OwnLocationOverlay ownLocationOverlay;

    private Persistor persistor;

    private boolean clearData;

    private float notificationDistanceLimit;

    private float vibrationDistanceLimit;

    private final Set<DataListener> dataListeners = new HashSet<DataListener>();

    private final Set<String> androidIdsForExtendedFunctionality = new HashSet<String>(Arrays.asList("e73c5a22934b5915"));

    private PackageInfo pInfo;

    private ButtonColumnHandler buttonColumnHandler;

    private HistoryController historyController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updatePackageInfo();

        setContentView(isDebugBuild() ? R.layout.main_debug : R.layout.main);

        warning = (TextView) findViewById(R.id.warning);

        status = (TextView) findViewById(R.id.status);

        setMapView((OwnMapView) findViewById(R.id.mapview));

        getMapView().setBuiltInZoomControls(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (getLastNonConfigurationInstance() == null) {
            persistor = new Persistor((LocationManager) getSystemService(Context.LOCATION_SERVICE), preferences, pInfo);
        } else {
            persistor = (Persistor) getLastNonConfigurationInstance();
        }

        strokesOverlay = persistor.getStrokesOverlay();
        strokesOverlay.setActivity(this);

        participantsOverlay = persistor.getParticipantsOverlay();
        participantsOverlay.setActivity(this);

        getMapView().addZoomListener(new OwnMapView.ZoomListener() {

            @Override
            public void onZoom(int zoomLevel) {
                strokesOverlay.updateZoomLevel(zoomLevel);
                participantsOverlay.updateZoomLevel(zoomLevel);
            }

        });

        List<Overlay> mapOverlays = getMapView().getOverlays();

        fadeOverlay = new FadeOverlay(strokesOverlay.getColorHandler());
        mapOverlays.add(fadeOverlay);
        mapOverlays.add(strokesOverlay);
        mapOverlays.add(participantsOverlay);

        dataHandler = persistor.getDataHandler();
        dataHandler.setDataListener(this);

        setHistoricStatusString();

        progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setVisibility(View.INVISIBLE);

        errorIndicator = (ImageView) findViewById(R.id.error_indicator);
        errorIndicator.setVisibility(View.INVISIBLE);

        timerTask = persistor.getTimerTask();
        timerTask.setListener(this);

        notificationHandler = new NotificationHandler(this);

        alarmManager = persistor.getAlarmManager();
        alarmManager.clearAlarmListeners();
        alarmManager.addAlarmListener(this);
        if (alarmManager.isAlarmEnabled()) {
            onAlarmResult(alarmManager.getAlarmStatus());
        }

        ownLocationOverlay = new OwnLocationOverlay(getBaseContext(), getMapView());

        buttonColumnHandler = new ButtonColumnHandler((RelativeLayout) findViewById(R.layout.map_overlay));
        historyController = new HistoryController(this, dataHandler, timerTask);
        historyController.setButtonHandler(buttonColumnHandler);
        buttonColumnHandler.addAllElements(historyController.getButtons());

        setupDebugModeButton();

        buttonColumnHandler.updateButtonColumn();

        setupCustomViews();

        onSharedPreferenceChanged(preferences, Preferences.MAP_TYPE_KEY);
        onSharedPreferenceChanged(preferences, Preferences.MAP_FADE_KEY);
        onSharedPreferenceChanged(preferences, Preferences.SHOW_LOCATION_KEY);
        onSharedPreferenceChanged(preferences, Preferences.NOTIFICATION_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, Preferences.VIBRATION_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, Preferences.DO_NOT_SLEEP);

        getMapView().invalidate();
    }

    private void setupDebugModeButton() {
        final String androidId = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (isDebugBuild() || (androidId != null && androidIdsForExtendedFunctionality.contains(androidId))) {
            ImageButton rasterToggle = (ImageButton) findViewById(R.id.toggleExtendedMode);
            rasterToggle.setEnabled(true);
            rasterToggle.setVisibility(View.VISIBLE);
            rasterToggle.getDrawable().setAlpha(40);
            rasterToggle.getBackground().setAlpha(40);

            rasterToggle.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    buttonColumnHandler.disableButtonColumn();
                    dataHandler.toggleExtendedMode();
                    onDataReset();
                }
            });
            buttonColumnHandler.addElement(rasterToggle);
        }
    }

    private void setupCustomViews() {
        LegendView legendView = (LegendView) findViewById(R.id.legend_view);
        strokesOverlay.setIntervalDuration(dataHandler.getIntervalDuration());
        legendView.setStrokesOverlay(strokesOverlay);
        legendView.setAlpha(150);

        AlarmView alarmView = (AlarmView) findViewById(R.id.alarm_view);
        alarmView.setAlarmManager(alarmManager);
        alarmView.setColorHandler(strokesOverlay.getColorHandler(), strokesOverlay.getIntervalDuration());
        alarmView.setBackgroundColor(Color.TRANSPARENT);
        alarmView.setAlpha(200);

        HistogramView histogramView = (HistogramView) findViewById(R.id.histogram_view);
        histogramView.setStrokesOverlay(strokesOverlay);
        if (persistor.hasCurrentResult()) {
            histogramView.onDataUpdate(persistor.getCurrentResult());
        }
        addDataListener(histogramView);
    }


    private void addDataListener(DataListener listener) {
        dataListeners.add(listener);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        strokesOverlay.clearPopup();
        participantsOverlay.clearPopup();
        return persistor;
    }

    public boolean isDebugBuild() {
        boolean dbg = false;
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);

            dbg = ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        } catch (Exception ignored) {
        }
        return dbg;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_info:
                showDialog(R.id.info_dialog);
                break;

            case R.id.menu_alarms:
                showDialog(R.id.alarm_dialog);
                break;

            case R.id.menu_preferences:
                startActivity(new Intent(this, Preferences.class));
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onResume() {
        super.onResume();
        timerTask.onResume(dataHandler.isRealtime());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(Preferences.SHOW_LOCATION_KEY, false)) {
            ownLocationOverlay.enableOwnLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        timerTask.onPause();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(Preferences.SHOW_LOCATION_KEY, false)) {
            ownLocationOverlay.disableOwnLocation();
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    public void onBeforeDataUpdate() {
        buttonColumnHandler.disableButtonColumn();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
    }

    @Override
    public void onDataUpdate(DataResult result) {

        historyController.setRealtimeData(result.containsRealtimeData());

        Parameters resultParameters = result.getParameters();

        persistor.setCurrentResult(result);

        clearDataIfRequested();

        if (result.containsStrokes()) {
            strokesOverlay.setRasterParameters(result.getRasterParameters());
            strokesOverlay.setRegion(resultParameters.getRegion());
            strokesOverlay.setReferenceTime(result.getReferenceTime());
            strokesOverlay.setIntervalDuration(resultParameters.getIntervalDuration());
            strokesOverlay.setIntervalOffset(resultParameters.getIntervalOffset());
            strokesOverlay.addStrokes(result.getStrokes());

            if (alarmManager.isAlarmEnabled()) {
                alarmManager.check(result);
            }
            strokesOverlay.refresh();
        }

        if (!result.containsRealtimeData()) {
            timerTask.disable();
            setHistoricStatusString();
        }

        if (participantsOverlay != null && result.containsParticipants()) {
            participantsOverlay.setParticipants(result.getParticipants());
            participantsOverlay.refresh();
        }

        for (DataListener listener : dataListeners) {
            listener.onDataUpdate(result);
        }

        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setProgress(progressBar.getMax());

        buttonColumnHandler.enableButtonColumn();
        getMapView().invalidate();
    }

    private void clearDataIfRequested() {
        if (clearData) {
            clearData = false;

            strokesOverlay.clear();

            if (participantsOverlay != null) {
                participantsOverlay.clear();
            }
        }
    }

    @Override
    public void onDataReset() {
        timerTask.restart();
        clearData = true;

        for (DataListener listener : dataListeners) {
            listener.onDataReset();
        }
    }

    @Override
    public void setErrorIndicator(boolean displayError) {
        errorIndicator.setVisibility(displayError ? View.VISIBLE : View.INVISIBLE);
    }


    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case R.id.info_dialog:
                dialog = new InfoDialog(this, pInfo);
                break;

            case R.id.alarm_dialog:
                dialog = new AlarmDialog(this, alarmManager, strokesOverlay.getColorHandler(), strokesOverlay.getIntervalDuration());
                break;

            default:
                dialog = null;
        }
        return dialog;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.MAP_TYPE_KEY)) {
            String mapTypeString = sharedPreferences.getString(Preferences.MAP_TYPE_KEY, "SATELLITE");
            getMapView().setSatellite(mapTypeString.equals("SATELLITE"));
            strokesOverlay.refresh();
            if (participantsOverlay != null) {
                participantsOverlay.refresh();
            }
        } else if (key.equals(Preferences.COLOR_SCHEME_KEY)) {
            strokesOverlay.refresh();
            if (participantsOverlay != null) {
                participantsOverlay.refresh();
            }
        } else if (key.equals(Preferences.MAP_FADE_KEY)) {
            int alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.getInt(Preferences.MAP_FADE_KEY, 40));
            fadeOverlay.setAlpha(alphaValue);
        } else if (key.equals(Preferences.RASTER_SIZE_KEY)) {
            timerTask.restart();
        } else if (key.equals(Preferences.REGION_KEY)) {
            timerTask.restart();
        } else if (key.equals(Preferences.NOTIFICATION_DISTANCE_LIMIT)) {
            notificationDistanceLimit = Float.parseFloat(sharedPreferences.getString(Preferences.NOTIFICATION_DISTANCE_LIMIT, "50"));
        } else if (key.equals(Preferences.VIBRATION_DISTANCE_LIMIT)) {
            vibrationDistanceLimit = Float.parseFloat(sharedPreferences.getString(Preferences.VIBRATION_DISTANCE_LIMIT, "25"));
        } else if (key.equals(Preferences.DO_NOT_SLEEP)) {
            boolean doNotSleep = sharedPreferences.getBoolean(key, false);
            int flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

            if (doNotSleep) {
                getWindow().addFlags(flag);
            } else {
                getWindow().clearFlags(flag);
            }

        }
    }

    public void onTimerUpdate(String timerStatus) {
        setStatusString(timerStatus);
    }

    protected void setHistoricStatusString() {
        if (!strokesOverlay.hasRealtimeData()) {
            long referenceTime = strokesOverlay.getReferenceTime() + strokesOverlay.getIntervalOffset() * 60 * 1000;
            String timeString = (String) DateFormat.format("@ kk:mm", referenceTime);
            setStatusString(timeString);
        }
    }

    protected void setStatusString(String runStatus) {
        int numberOfStrokes = strokesOverlay.getTotalNumberOfStrokes();
        String statusString = getResources().getQuantityString(R.plurals.stroke, numberOfStrokes, numberOfStrokes);
        statusString += "/";
        int intervalDuration = strokesOverlay.getIntervalDuration();
        statusString += getResources().getQuantityString(R.plurals.minute, intervalDuration, intervalDuration);
        statusString += " " + runStatus;

        int region = strokesOverlay.getRegion();
        if (region != 0) {
            String regions[] = getResources().getStringArray(R.array.regions_values);
            int index = 0;
            for (String region_number : regions) {
                if (region == Integer.parseInt(region_number)) {
                    statusString += " " + getResources().getStringArray(R.array.regions)[index];
                    break;
                }
                index++;
            }
        }

        status.setText(statusString);
    }

    @Override
    public void onAlarmResult(AlarmStatus alarmStatus) {
        AlarmLabel alarmLabel = new AlarmLabel(warning, getResources());

        alarmLabel.apply(alarmStatus);

        if (alarmStatus != null) {
            if (alarmStatus.getClosestStrokeDistance() <= vibrationDistanceLimit) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(40);
            }

            if (alarmStatus.getClosestStrokeDistance() <= notificationDistanceLimit) {
                notificationHandler.sendNotification(getResources().getString(R.string.activity) + ": " + alarmStatus.getTextMessage(notificationDistanceLimit));
            } else {
                notificationHandler.clearNotification();
            }
        } else {
            notificationHandler.clearNotification();
        }
    }

    @Override
    public void onAlarmClear() {
        warning.setText("");
    }

    private void updatePackageInfo() {
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}