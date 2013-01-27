package org.blitzortung.android.app;

import android.app.Dialog;
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
import org.blitzortung.android.alarm.AlarmLabelHandler;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmStatus;
import org.blitzortung.android.app.controller.ButtonColumnHandler;
import org.blitzortung.android.app.controller.HistoryController;
import org.blitzortung.android.app.controller.NotificationHandler;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.app.view.HistogramView;
import org.blitzortung.android.app.view.LegendView;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.app.view.components.StatusComponent;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.dialogs.AlarmDialog;
import org.blitzortung.android.dialogs.InfoDialog;
import org.blitzortung.android.dialogs.LayerDialog;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.FadeOverlay;
import org.blitzortung.android.map.overlay.OwnLocationOverlay;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;

import java.util.*;

public class Main extends OwnMapActivity implements DataListener, OnSharedPreferenceChangeListener, TimerTask.TimerUpdateListener,
        AlarmManager.AlarmListener {

    protected StatusComponent statusComponent;

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

    private final Set<String> androidIdsForExtendedFunctionality = new HashSet<String>(Arrays.asList("e73c5a22934b5915", "e72d101ce1bcdee3"));

    private PackageInfo pInfo;

    private ButtonColumnHandler<ImageButton> buttonColumnHandler;

    private HistoryController historyController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updatePackageInfo();

        setContentView(isDebugBuild() ? R.layout.main_debug : R.layout.main);

        OwnMapView mapView = (OwnMapView) findViewById(R.id.mapview);
        setMapView(mapView);

        mapView.setBuiltInZoomControls(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (getLastNonConfigurationInstance() == null) {
            persistor = new Persistor(this, preferences, pInfo);
        } else {
            persistor = (Persistor) getLastNonConfigurationInstance();
        }
        persistor.updateContext(this);

        strokesOverlay = persistor.getStrokesOverlay();
        participantsOverlay = persistor.getParticipantsOverlay();

        mapView.addZoomListener(new OwnMapView.ZoomListener() {

            @Override
            public void onZoom(int zoomLevel) {
                strokesOverlay.updateZoomLevel(zoomLevel);
                participantsOverlay.updateZoomLevel(zoomLevel);
            }

        });

        fadeOverlay = new FadeOverlay(strokesOverlay.getColorHandler());

        addOverlays(fadeOverlay, strokesOverlay, participantsOverlay);

        dataHandler = persistor.getDataHandler();
        setHistoricStatusString();
        timerTask = persistor.getTimerTask();

        notificationHandler = new NotificationHandler(this);

        alarmManager = persistor.getAlarmManager();

        statusComponent = new StatusComponent(this);

        if (alarmManager.isAlarmEnabled()) {
            onAlarmResult(alarmManager.getAlarmStatus());
        }

        ownLocationOverlay = new OwnLocationOverlay(getBaseContext(), getMapView());

        buttonColumnHandler = new ButtonColumnHandler<ImageButton>((RelativeLayout) findViewById(R.layout.map_overlay));
        historyController = new HistoryController(this, dataHandler, timerTask);
        historyController.setButtonHandler(buttonColumnHandler);
        buttonColumnHandler.addAllElements(historyController.getButtons());

        setupDebugModeButton();

        buttonColumnHandler.updateButtonColumn();

        setupCustomViews();

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION,
                PreferenceKey.NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.VIBRATION_DISTANCE_LIMIT, PreferenceKey.DO_NOT_SLEEP);

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

            case R.id.menu_layers:
                showDialog(R.id.layer_dialog);
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
        if (preferences.getBoolean(PreferenceKey.SHOW_LOCATION.toString(), false)) {
            ownLocationOverlay.enableOwnLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        timerTask.onPause();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(PreferenceKey.SHOW_LOCATION.toString(), false)) {
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
        statusComponent.startProgress();
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

        statusComponent.stopProgress();

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
    public void setErrorIndicator(boolean indicateError) {
        statusComponent.indicateError(indicateError);
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

            case R.id.layer_dialog:
                dialog = new LayerDialog(this, getMapView());
                break;

            default:
                dialog = null;
        }
        return dialog;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey... keys) {
        for (PreferenceKey key : keys) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case MAP_TYPE:
                String mapTypeString = sharedPreferences.getString(key.toString(), "SATELLITE");
                getMapView().setSatellite(mapTypeString.equals("SATELLITE"));
                strokesOverlay.refresh();
                if (participantsOverlay != null) {
                    participantsOverlay.refresh();
                }
                break;

            case COLOR_SCHEME:
                strokesOverlay.refresh();
                if (participantsOverlay != null) {
                    participantsOverlay.refresh();
                }
                break;

            case MAP_FADE:
                int alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.getInt(key.toString(), 40));
                fadeOverlay.setAlpha(alphaValue);
                break;

            case RASTER_SIZE:
            case REGION:
                timerTask.restart();
                break;

            case NOTIFICATION_DISTANCE_LIMIT:
                notificationDistanceLimit = Float.parseFloat(sharedPreferences.getString(key.toString(), "50"));
                break;

            case VIBRATION_DISTANCE_LIMIT:
                vibrationDistanceLimit = Float.parseFloat(sharedPreferences.getString(key.toString(), "25"));
                break;

            case DO_NOT_SLEEP:
                boolean doNotSleep = sharedPreferences.getBoolean(key.toString(), false);
                int flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

                if (doNotSleep) {
                    getWindow().addFlags(flag);
                } else {
                    getWindow().clearFlags(flag);
                }
                break;

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
        String statusText = getResources().getQuantityString(R.plurals.stroke, numberOfStrokes, numberOfStrokes);
        statusText += "/";
        int intervalDuration = strokesOverlay.getIntervalDuration();
        statusText += getResources().getQuantityString(R.plurals.minute, intervalDuration, intervalDuration);
        statusText += " " + runStatus;

        int region = strokesOverlay.getRegion();
        if (region != 0) {
            String regions[] = getResources().getStringArray(R.array.regions_values);
            int index = 0;
            for (String region_number : regions) {
                if (region == Integer.parseInt(region_number)) {
                    statusText += " " + getResources().getStringArray(R.array.regions)[index];
                    break;
                }
                index++;
            }
        }

        statusComponent.setText(statusText);

    }

    @Override
    public void onAlarmResult(AlarmStatus alarmStatus) {
        AlarmLabelHandler alarmLabelHandler = new AlarmLabelHandler(statusComponent, getResources());

        alarmLabelHandler.apply(alarmStatus);

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
        statusComponent.setAlarmText("");
    }

    private void updatePackageInfo() {
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}