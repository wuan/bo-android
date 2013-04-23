package org.blitzortung.android.app;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import org.blitzortung.android.alarm.AlarmLabelHandler;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.app.controller.ButtonColumnHandler;
import org.blitzortung.android.app.controller.HistoryController;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.app.view.HistogramView;
import org.blitzortung.android.app.view.LegendView;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.app.view.components.StatusComponent;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.dialogs.AlarmDialog;
import org.blitzortung.android.dialogs.AlarmDialogColorHandler;
import org.blitzortung.android.dialogs.InfoDialog;
import org.blitzortung.android.dialogs.LayerDialog;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.FadeOverlay;
import org.blitzortung.android.map.overlay.OwnLocationOverlay;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main extends OwnMapActivity implements DataListener, OnSharedPreferenceChangeListener, TimerTask.TimerUpdateListener,
        AlarmManager.AlarmListener {

    protected StatusComponent statusComponent;

    private FadeOverlay fadeOverlay;

    protected StrokesOverlay strokesOverlay;

    private ParticipantsOverlay participantsOverlay;

    private TimerTask timerTask;

    private AlarmManager alarmManager;

    private LocationHandler locationHandler;

    private DataHandler dataHandler;

    private OwnLocationOverlay ownLocationOverlay;

    private Persistor persistor;

    private boolean clearData;

    private final Set<DataListener> dataListeners = new HashSet<DataListener>();

    private final Set<String> androidIdsForExtendedFunctionality = new HashSet<String>(Arrays.asList("5cba4df1f0ad9e75", "e72d101ce1bcdee3"));

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

        locationHandler = persistor.getLocationHandler();
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

        ownLocationOverlay = new OwnLocationOverlay(getBaseContext(), persistor.getLocationHandler(), getMapView());

        addOverlays(fadeOverlay, strokesOverlay, participantsOverlay, ownLocationOverlay);

        dataHandler = persistor.getDataHandler();
        statusComponent = new StatusComponent(this);
        setHistoricStatusString();
        timerTask = persistor.getTimerTask();

        alarmManager = persistor.getAlarmManager();

        if (alarmManager.isAlarmEnabled()) {
            onAlarmResult(alarmManager.getAlarmResult());
        }

        buttonColumnHandler = new ButtonColumnHandler<ImageButton>((RelativeLayout) findViewById(R.layout.map_overlay));

        if (Build.VERSION.SDK_INT >= 11) {

            ImageButton menuButton = (ImageButton) findViewById(R.id.menu);
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openOptionsMenu();
                }
            });
            buttonColumnHandler.addElement(menuButton);

            //noinspection EmptyCatchBlock
            try {
                Method getActionBar = Main.class.getMethod("getActionBar");

                Object actionBar;
                actionBar = getActionBar.invoke(this);

                Method hide = actionBar.getClass().getMethod("hide");
                hide.invoke(actionBar);

            } catch (NoSuchMethodException e) {
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }
        }

        historyController = new HistoryController(this, dataHandler, timerTask);
        historyController.setButtonHandler(buttonColumnHandler);
        if (persistor.hasCurrentResult()) {
            historyController.setRealtimeData(persistor.getCurrentResult().containsRealtimeData());
        }
        buttonColumnHandler.addAllElements(historyController.getButtons());

        setupDebugModeButton();

        buttonColumnHandler.updateButtonColumn();

        setupCustomViews();

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION,
                PreferenceKey.NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.SIGNALING_DISTANCE_LIMIT, PreferenceKey.DO_NOT_SLEEP, PreferenceKey.SHOW_PARTICIPANTS);

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
        alarmView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (alarmManager.isAlarmEnabled()) {
                    final Location currentLocation = alarmManager.getCurrentLocation();
                    if (currentLocation != null) {
                        float radius = alarmManager.getMaxDistance();

                        final AlarmResult alarmResult = alarmManager.getAlarmResult();
                        if (alarmResult != null) {
                            radius = Math.max(Math.min(alarmResult.getClosestStrokeDistance() * 1.2f, radius), 50f) * 2f;
                        }

                        float diameter = 2 * radius;

                        animateToLocationAndVisibleSize(currentLocation.getLongitude(), currentLocation.getLatitude(), diameter);
                    }

                }
            }
        });

        HistogramView histogramView = (HistogramView) findViewById(R.id.histogram_view);
        histogramView.setStrokesOverlay(strokesOverlay);
        if (persistor.hasCurrentResult()) {
            histogramView.onDataUpdate(persistor.getCurrentResult());
        }
        addDataListener(histogramView);
        histogramView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (persistor.hasCurrentResult()) {
                    final DataResult currentResult = persistor.getCurrentResult();
                    if (currentResult.hasRasterParameters()) {
                        final RasterParameters rasterParameters = currentResult.getRasterParameters();
                        animateToLocationAndVisibleSize(rasterParameters.getRectCenterLongitude(), rasterParameters.getRectCenterLatitude(), 5000f);
                    }
                }
            }
        });
    }

    private void animateToLocationAndVisibleSize(double longitude, double latitude, float diameter) {
        final OwnMapView mapView = getMapView();
        final MapController controller = mapView.getController();

        final int startZoomLevel = mapView.getZoomLevel();
        final int targetZoomLevel = mapView.calculateTargetZoomLevel(diameter * 1000f);

        controller.animateTo(new GeoPoint((int) (latitude * 1e6), (int) (longitude * 1e6)), new Runnable() {
            @Override
            public void run() {
                if (startZoomLevel != targetZoomLevel) {
                    final boolean zoomOut = targetZoomLevel - startZoomLevel < 0;
                    final int zoomCount = Math.abs(targetZoomLevel - startZoomLevel);
                    Handler handler = new Handler();
                    long delay = 0;
                    for (int i = 0; i < zoomCount; i++) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (zoomOut) {
                                    controller.zoomOut();
                                } else {
                                    controller.zoomIn();
                                }
                            }
                        }, delay);
                        delay += 150;
                    }
                }
            }
        });
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

            /*case R.id.menu_layers:
                showDialog(R.id.layer_dialog);
                break;*/

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
        locationHandler.onResume();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(PreferenceKey.SHOW_LOCATION.toString(), false)) {
            ownLocationOverlay.enableOwnLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (timerTask.onPause()) {
            locationHandler.onPause();
        }

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
        statusComponent.indicateError(false);

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

            if (result.containsIncrementalData()) {
                strokesOverlay.expireStrokes();
            } else {
                strokesOverlay.clear();
            }
            strokesOverlay.addStrokes(result.getStrokes());

            alarmManager.checkStrokes(strokesOverlay.getStrokes(), result.containsRealtimeData());

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
    public void onDataError() {
        statusComponent.indicateError(true);
        statusComponent.stopProgress();

        buttonColumnHandler.enableButtonColumn();
    }


    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case R.id.info_dialog:
                dialog = new InfoDialog(this, pInfo);
                break;

            case R.id.alarm_dialog:
                dialog = new AlarmDialog(this, alarmManager, new AlarmDialogColorHandler(PreferenceManager.getDefaultSharedPreferences(this)), strokesOverlay.getIntervalDuration());
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

            case SHOW_PARTICIPANTS:
                boolean showParticipants = sharedPreferences.getBoolean(key.toString(), true);
                participantsOverlay.setEnabled(showParticipants);
                updateOverlays();
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
                if (timerTask.isEnabled()) {
                    timerTask.restart();
                } else {
                    Set<DataChannel> updateTargets = new HashSet<DataChannel>();
                    updateTargets.add(DataChannel.STROKES);
                    dataHandler.updateData(updateTargets);
                }
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
    public void onAlarmResult(AlarmResult alarmResult) {
        AlarmLabelHandler alarmLabelHandler = new AlarmLabelHandler(statusComponent, getResources());

        alarmLabelHandler.apply(alarmResult);
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