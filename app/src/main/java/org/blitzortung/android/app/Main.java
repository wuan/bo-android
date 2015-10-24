package org.blitzortung.android.app;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.*;
import android.widget.ImageButton;
import android.widget.Toast;

import com.annimon.stream.function.Consumer;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;

import org.blitzortung.android.alert.AlertHandler;
import org.blitzortung.android.alert.AlertResult;
import org.blitzortung.android.app.components.VersionComponent;
import org.blitzortung.android.app.controller.ButtonColumnHandler;
import org.blitzortung.android.app.controller.HistoryController;
import org.blitzortung.android.app.view.AlertView;
import org.blitzortung.android.app.view.HistogramView;
import org.blitzortung.android.app.view.LegendView;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.app.view.components.StatusComponent;
import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.provider.result.*;
import org.blitzortung.android.dialogs.*;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.FadeOverlay;
import org.blitzortung.android.map.overlay.OwnLocationOverlay;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrikesOverlay;
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler;
import org.blitzortung.android.map.overlay.color.StrikeColorHandler;
import org.blitzortung.android.util.optional.Optional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main extends OwnMapActivity implements OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = "BO_ANDROID";

    protected StatusComponent statusComponent;

    private FadeOverlay fadeOverlay;

    protected StrikesOverlay strikesOverlay;

    private ParticipantsOverlay participantsOverlay;

    private OwnLocationOverlay ownLocationOverlay;

    private boolean clearData;

    private final Set<String> androidIdsForExtendedFunctionality = new HashSet<>(Arrays.asList("e72d101ce1bcdee3", "6d1b9a3da993af2d"));

    private ButtonColumnHandler<ImageButton> buttonColumnHandler;

    private HistoryController historyController;
    private AppService appService;
    private ServiceConnection serviceConnection;
    private LegendView legendView;
    private AlertView alertView;

    private Optional<ResultEvent> currentResult;
    private HistogramView histogramView;
    private VersionComponent versionComponent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (NoClassDefFoundError e) {
            Log.e(Main.LOG_TAG, e.toString());
            Toast.makeText(getBaseContext(), "bad android version", Toast.LENGTH_LONG).show();
        }
        Log.v(LOG_TAG, "Main.onCreate()");

        versionComponent = new VersionComponent(this.getApplicationContext());

        currentResult = Optional.absent();

        setContentView(isDebugBuild() ? R.layout.main_debug : R.layout.main);

        OwnMapView mapView = (OwnMapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        setMapView(mapView);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        strikesOverlay = new StrikesOverlay(this, new StrikeColorHandler(preferences));
        participantsOverlay = new ParticipantsOverlay(this, new ParticipantColorHandler(preferences));

        mapView.addZoomListener(zoomLevel -> {
            strikesOverlay.updateZoomLevel(zoomLevel);
            participantsOverlay.updateZoomLevel(zoomLevel);
        });

        fadeOverlay = new FadeOverlay(strikesOverlay.getColorHandler());
        ownLocationOverlay = new OwnLocationOverlay(getBaseContext(), getMapView());

        addOverlay(fadeOverlay);
        addOverlay(strikesOverlay);
        addOverlay(participantsOverlay);
        addOverlay(ownLocationOverlay);
        updateOverlays();

        statusComponent = new StatusComponent(this);
        setHistoricStatusString();

        hideActionBar();

        buttonColumnHandler = new ButtonColumnHandler<>();
        configureMenuAccess();
        historyController = new HistoryController(this);
        historyController.setButtonHandler(buttonColumnHandler);

        buttonColumnHandler.addAllElements(historyController.getButtons());

        setupDebugModeButton();

        buttonColumnHandler.lockButtonColumn();
        buttonColumnHandler.updateButtonColumn();

        setupCustomViews();

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION,
                PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT, PreferenceKey.DO_NOT_SLEEP, PreferenceKey.SHOW_PARTICIPANTS);

        createAndBindToDataService();

        if (versionComponent.getState() == VersionComponent.State.FIRST_RUN) {
            openQuickSettingsDialog();
        }
    }

    private void createAndBindToDataService() {
        final Intent serviceIntent = new Intent(this, AppService.class);

        startService(serviceIntent);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                appService = ((AppService.DataServiceBinder) iBinder).getService();
                Log.i(Main.LOG_TAG, "Main.ServiceConnection.onServiceConnected() " + appService);

                setupService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };

        bindService(serviceIntent, serviceConnection, 0);
    }

    private void setupService() {
        if (appService != null) {
            historyController.setAppService(appService);
            appService.addDataConsumer(historyController.getDataConsumer());
            appService.addDataConsumer(getDataEventConsumer());

            appService.addLocationConsumer(ownLocationOverlay.getLocationEventConsumer());
            appService.addDataConsumer(histogramView.getDataConsumer());

            appService.addLocationConsumer(alertView.getLocationEventConsumer());
            appService.addAlertConsumer(alertView.getAlertEventConsumer());

            appService.addAlertConsumer(statusComponent.getAlertEventConsumer());
        }
    }

    private void setupDebugModeButton() {
        final String androidId = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (isDebugBuild() || (androidId != null && androidIdsForExtendedFunctionality.contains(androidId))) {
            ImageButton rasterToggle = (ImageButton) findViewById(R.id.toggleExtendedMode);
            rasterToggle.setEnabled(true);
            rasterToggle.setVisibility(View.VISIBLE);

            rasterToggle.setOnClickListener(v -> {
                buttonColumnHandler.lockButtonColumn();
                appService.getDataHandler().toggleExtendedMode();
                reloadData();
            });
            buttonColumnHandler.addElement(rasterToggle);
        }
    }

    private void setupCustomViews() {
        legendView = (LegendView) findViewById(R.id.legend_view);
        legendView.setStrikesOverlay(strikesOverlay);
        legendView.setAlpha(150);
        legendView.setOnClickListener(v -> openQuickSettingsDialog());

        alertView = (AlertView) findViewById(R.id.alert_view);
        alertView.setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.getIntervalDuration());
        alertView.setBackgroundColor(Color.TRANSPARENT);
        alertView.setAlpha(200);
        alertView.setOnClickListener(view -> {
            final AlertHandler alertHandler = appService.getAlertHandler();
            if (alertHandler != null && alertHandler.isAlertEnabled()) {
                final Location currentLocation = alertHandler.getCurrentLocation();
                if (currentLocation != null) {
                    float radius = alertHandler.getMaxDistance();

                    final AlertResult alertResult = alertHandler.getAlarmResult();
                    if (alertResult != null) {
                        radius = Math.max(Math.min(alertResult.getClosestStrikeDistance() * 1.2f, radius), 50f);
                    }

                    float diameter = 1.5f * 2f * radius;
                    animateToLocationAndVisibleSize(currentLocation.getLongitude(), currentLocation.getLatitude(), diameter);
                }

            }
        });

        histogramView = (HistogramView) findViewById(R.id.histogram_view);
        histogramView.setStrikesOverlay(strikesOverlay);
        histogramView.setOnClickListener(view -> {
            if (currentResult.isPresent()) {
                final ResultEvent result = currentResult.get();
                if (result.hasRasterParameters()) {
                    final RasterParameters rasterParameters = result.getRasterParameters();
                    animateToLocationAndVisibleSize(rasterParameters.getRectCenterLongitude(), rasterParameters.getRectCenterLatitude(), 5000f);
                } else {
                    animateToLocationAndVisibleSize(0, 0, 20000f);
                }
            }
        });
    }

    private void openQuickSettingsDialog() {
        DialogFragment dialog = new QuickSettingsDialog();
        dialog.show(getFragmentManager(), "QuickSettingsDialog");
    }

    private void animateToLocationAndVisibleSize(double longitude, double latitude, float diameter) {
        Log.d(Main.LOG_TAG, String.format("Main.animateAndZoomTo() %.4f, %.4f, %.0fkm", longitude, latitude, diameter));

        final OwnMapView mapView = getMapView();
        final MapController controller = mapView.getController();

        final int startZoomLevel = mapView.getZoomLevel();
        final int targetZoomLevel = mapView.calculateTargetZoomLevel(diameter * 1000f);

        controller.animateTo(new GeoPoint((int) (latitude * 1e6), (int) (longitude * 1e6)), () -> {
            if (startZoomLevel != targetZoomLevel) {
                final boolean zoomOut = targetZoomLevel - startZoomLevel < 0;
                final int zoomCount = Math.abs(targetZoomLevel - startZoomLevel);
                Handler handler = new Handler();
                long delay = 0;
                for (int i = 0; i < zoomCount; i++) {
                    handler.postDelayed(() -> {
                        if (zoomOut) {
                            controller.zoomOut();
                        } else {
                            controller.zoomIn();
                        }
                    }, delay);
                    delay += 150;
                }
            }
        });
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
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
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
    public void onStart() {
        super.onStart();

        setupService();

        Log.d(Main.LOG_TAG, "Main.onStart() service: " + appService);
    }

    @Override
    public void onRestart() {
        super.onRestart();

        Log.d(Main.LOG_TAG, "Main.onStart() service: " + appService);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(Main.LOG_TAG, "Main.onResume() service: " + appService);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(Main.LOG_TAG, "Main.onPause()");
    }

    @Override
    public void onStop() {
        super.onStop();

        if (appService != null) {
            Log.v(Main.LOG_TAG, "Main.onStop() remove listeners");

            historyController.setAppService(null);
            appService.removeDataConsumer(historyController.getDataConsumer());
            appService.removeDataConsumer(getDataEventConsumer());

            appService.removeLocationConsumer(ownLocationOverlay.getLocationEventConsumer());
            appService.removeDataConsumer(histogramView.getDataConsumer());

            appService.removeLocationConsumer(alertView.getLocationEventConsumer());
            appService.removeAlertListener(alertView.getAlertEventConsumer());

            appService.removeAlertListener(statusComponent.getAlertEventConsumer());
        } else {
            Log.i(LOG_TAG, "Main.onStop()");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Main: onDestroy() unbind service");

        unbindService(serviceConnection);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private Consumer<DataEvent> dataEventConsumer = new Consumer<DataEvent>() {
        @Override
        public void accept(DataEvent event) {
            if (event instanceof RequestStartedEvent) {
                buttonColumnHandler.lockButtonColumn();
                statusComponent.startProgress();
            } else if (event instanceof ResultEvent) {
                ResultEvent result = (ResultEvent) event;

                if (result.hasFailed()) {
                    statusComponent.indicateError(true);
                } else {
                    statusComponent.indicateError(false);

                    if (result.getParameters().getIntervalDuration() != appService.getDataHandler().getIntervalDuration()) {
                        reloadData();
                    }

                    currentResult = Optional.of(result);

                    Log.d(Main.LOG_TAG, "Main.onDataUpdate() " + result);

                    Parameters resultParameters = result.getParameters();

                    clearDataIfRequested();

                    if (result.containsStrikes()) {
                        strikesOverlay.setParameters(resultParameters);
                        strikesOverlay.setRasterParameters(result.getRasterParameters());
                        strikesOverlay.setReferenceTime(result.getReferenceTime());

                        if (result.containsIncrementalData()) {
                            strikesOverlay.expireStrikes();
                        } else {
                            strikesOverlay.clear();
                        }
                        strikesOverlay.addStrikes(result.getStrikes());

                        alertView.setColorHandler(strikesOverlay.getColorHandler(), strikesOverlay.getIntervalDuration());

                        strikesOverlay.refresh();
                        legendView.requestLayout();
                    }

                    if (!result.containsRealtimeData()) {
                        setHistoricStatusString();
                    }

                    if (participantsOverlay != null && result.containsParticipants()) {
                        participantsOverlay.setParticipants(result.getStations());
                        participantsOverlay.refresh();
                    }
                }

                statusComponent.stopProgress();

                buttonColumnHandler.unlockButtonColumn();

                getMapView().invalidate();
                legendView.invalidate();
            } else if (event instanceof ClearDataEvent) {
                clearData();
            } else if (event instanceof StatusEvent) {
                StatusEvent statusEvent = (StatusEvent) event;
                setStatusString(statusEvent.getStatus());
            }
        }
    };

    public Consumer<DataEvent> getDataEventConsumer() {
        return dataEventConsumer;
    }

    private void reloadData() {
        appService.reloadData();
    }

    private void clearDataIfRequested() {
        if (clearData) {
            clearData();
        }
    }

    private void clearData() {
        clearData = false;

        strikesOverlay.clear();

        if (participantsOverlay != null) {
            participantsOverlay.clear();
        }
    }

    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case R.id.info_dialog:
                dialog = new InfoDialog(this, versionComponent);
                break;

            case R.id.alarm_dialog:
                if (appService != null) {
                    dialog = new AlertDialog(this, appService, new AlertDialogColorHandler(PreferenceManager.getDefaultSharedPreferences(this)));
                }
                break;
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
                strikesOverlay.refresh();
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
                strikesOverlay.refresh();
                if (participantsOverlay != null) {
                    participantsOverlay.refresh();
                }
                break;

            case MAP_FADE:
                int alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.getInt(key.toString(), 40));
                fadeOverlay.setAlpha(alphaValue);
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

    protected void setHistoricStatusString() {
        if (!strikesOverlay.hasRealtimeData()) {
            long referenceTime = strikesOverlay.getReferenceTime() + strikesOverlay.getIntervalOffset() * 60 * 1000;
            String timeString = (String) DateFormat.format("@ kk:mm", referenceTime);
            setStatusString(timeString);
        }
    }

    protected void setStatusString(String runStatus) {
        int numberOfStrikes = strikesOverlay.getTotalNumberOfStrikes();
        String statusText = getResources().getQuantityString(R.plurals.strike, numberOfStrikes, numberOfStrikes);
        statusText += "/";
        int intervalDuration = strikesOverlay.getIntervalDuration();
        statusText += getResources().getQuantityString(R.plurals.minute, intervalDuration, intervalDuration);
        statusText += " " + runStatus;

        statusComponent.setText(statusText);
    }

    private void configureMenuAccess() {
        ViewConfiguration config = ViewConfiguration.get(this);

        if (!config.hasPermanentMenuKey()) {
            ImageButton menuButton = (ImageButton) findViewById(R.id.menu);
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setOnClickListener(v -> openOptionsMenu());
            buttonColumnHandler.addElement(menuButton);
        }
    }

    private void hideActionBar() {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }
}