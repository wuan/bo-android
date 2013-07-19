package org.blitzortung.android.app;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.*;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.*;

import org.blitzortung.android.alarm.AlarmLabelHandler;
import org.blitzortung.android.alarm.LightningActivityAlarmManager;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.app.controller.ButtonColumnHandler;
import org.blitzortung.android.app.controller.HistoryController;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.controller.MapController;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.app.view.HistogramView;
import org.blitzortung.android.app.view.LegendView;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.app.view.components.StatusComponent;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.component.DataComponent;
import org.blitzortung.android.data.component.ParticipantsComponent;
import org.blitzortung.android.data.component.SpatialComponent;
import org.blitzortung.android.data.component.StrokesComponent;
import org.blitzortung.android.data.component.TimeComponent;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.dialogs.*;
import org.blitzortung.android.map.color.ColorHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main extends Activity implements DataListener, OnSharedPreferenceChangeListener, DataService.DataServiceStatusListener,
        LightningActivityAlarmManager.AlarmListener {

    public static final String LOG_TAG = "BO_ANDROID";

    protected StatusComponent statusComponent;

    protected StrokesComponent strokesComponent;

    private ParticipantsComponent participantsComponent;

    private LightningActivityAlarmManager lightningActivityAlarmManager;

    private LocationHandler locationHandler;

    private DataHandler dataHandler;

    private Persistor persistor;

    private GoogleMap map;

    private boolean clearData;

    private final Set<DataListener> dataListeners = new HashSet<DataListener>();

    private final Set<String> androidIdsForExtendedFunctionality = new HashSet<String>(Arrays.asList("e72d101ce1bcdee3", "6d1b9a3da993af2d"));

    private PackageInfo pInfo;

    private ButtonColumnHandler<ImageButton> buttonColumnHandler;

    private HistoryController historyController;
    private DataService dataService;
    private ServiceConnection serviceConnection;
    private LegendView legendView;
    private MapController mapController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (NoClassDefFoundError e) {
            Log.e(Main.LOG_TAG, e.toString());
            Toast toast = Toast.makeText(getBaseContext(), "bad android version", 1000);
            toast.show();
        }
        updatePackageInfo();

        setContentView(R.layout.main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (getLastNonConfigurationInstance() == null) {
            Log.i(Main.LOG_TAG, "Main.onCreate(): create new persistor");
            persistor = new Persistor(this, preferences, pInfo);
        } else {
            Log.i(Main.LOG_TAG, "Main.onCreate(): reuse persistor");
            persistor = (Persistor) getLastNonConfigurationInstance();
        }
        persistor.updateContext(this);

        locationHandler = persistor.getLocationHandler();
        strokesComponent = persistor.getStrokesComponent();
        participantsComponent = persistor.getParticipantsComponent();

        dataHandler = persistor.getDataHandler();
        statusComponent = new StatusComponent(this);
        setHistoricStatusString();

        lightningActivityAlarmManager = persistor.getLightningActivityAlarmManager();

        if (lightningActivityAlarmManager.isAlarmEnabled()) {
            onAlarmResult(lightningActivityAlarmManager.getAlarmResult());
        }

        buttonColumnHandler = new ButtonColumnHandler<ImageButton>();

        if (Build.VERSION.SDK_INT >= 14) {
            ViewConfiguration config = ViewConfiguration.get(this);

            if (!config.hasPermanentMenuKey()) {
                ImageButton menuButton = (ImageButton) findViewById(R.id.menu);
                menuButton.setVisibility(View.VISIBLE);
                menuButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        openOptionsMenu();
                    }
                });
                buttonColumnHandler.addElement(menuButton);
            }

            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.v2map);
        mapController = new MapController(mapFragment, preferences, getResources(), strokesComponent);
        if (persistor.hasCurrentResult()) {
            DataResult currentResult = persistor.getCurrentResult();
            mapController.updateMap(currentResult.data(), currentResult.getTime(), currentResult.getSpatial().getRasterParameters(), strokesComponent.getColorHandler());
        }

        historyController = new HistoryController(this, dataHandler);
        historyController.setButtonHandler(buttonColumnHandler);
        if (persistor.hasCurrentResult()) {
            historyController.setRealtimeData(persistor.getCurrentResult().getTime().isRealtime());
        }
        buttonColumnHandler.addAllElements(historyController.getButtons());

        setupDebugModeButton();

        buttonColumnHandler.updateButtonColumn();

        setupCustomViews();

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION,
                PreferenceKey.NOTIFICATION_DISTANCE_LIMIT, PreferenceKey.SIGNALING_DISTANCE_LIMIT, PreferenceKey.DO_NOT_SLEEP, PreferenceKey.SHOW_PARTICIPANTS);

        createAndBindToDataService();
    }

    private void createAndBindToDataService() {
        final Intent serviceIntent = new Intent(this, DataService.class);

        startService(serviceIntent);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                dataService = ((DataService.DataServiceBinder) iBinder).getService();
                Log.i(Main.LOG_TAG, "Main.ServiceConnection.onServiceConnected() " + dataService);
                dataService.setListener(Main.this);
                dataService.setDataHandler(dataHandler);
                if (!persistor.hasCurrentResult()) {
                    dataService.restart();
                }
                dataService.onResume();
                historyController.setDataService(dataService);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };

        bindService(serviceIntent, serviceConnection, 0);
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
        legendView = (LegendView) findViewById(R.id.legend_view);
        strokesComponent.setIntervalDuration(dataHandler.getIntervalDuration());
        legendView.setStrokesComponent(strokesComponent);
        legendView.setAlpha(150);
        legendView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(R.layout.settings_dialog);
                Log.v(LOG_TAG, "LegendView.onClick()");
            }
        });

        AlarmView alarmView = (AlarmView) findViewById(R.id.alarm_view);
        alarmView.setLightningActivityAlarmManager(lightningActivityAlarmManager);
        alarmView.setColorHandler(strokesComponent.getColorHandler(), strokesComponent.getIntervalDuration());
        alarmView.setBackgroundColor(Color.TRANSPARENT);
        alarmView.setAlpha(200);
        alarmView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lightningActivityAlarmManager.isAlarmEnabled()) {
                    final Location currentLocation = lightningActivityAlarmManager.getCurrentLocation();
                    if (currentLocation != null) {
                        float radius = lightningActivityAlarmManager.getMaxDistance();

                        final AlarmResult alarmResult = lightningActivityAlarmManager.getAlarmResult();
                        if (alarmResult != null) {
                            radius = Math.max(Math.min(alarmResult.getClosestStrokeDistance() * 1.2f, radius), 50f);
                        }

                        float diameter = 1.5f * 2f * radius;
                        animateToLocationAndVisibleSize((float)currentLocation.getLongitude(), (float)currentLocation.getLatitude(), diameter);
                    }

                }
            }
        });

        HistogramView histogramView = (HistogramView) findViewById(R.id.histogram_view);
        histogramView.setStrokesComponent(strokesComponent);
        if (persistor.hasCurrentResult()) {
            histogramView.onDataUpdate(persistor.getCurrentResult());
        }
        addDataListener(histogramView);
        histogramView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (persistor.hasCurrentResult()) {
                    final SpatialComponent spatial = persistor.getCurrentResult().getSpatial();
                    if (spatial.hasRasterParameters()) {
                        final RasterParameters rasterParameters = spatial.getRasterParameters();
                        animateToLocationAndVisibleSize(rasterParameters.getRectCenterLongitude(), rasterParameters.getRectCenterLatitude(), 5000f);
                    } else {
                        animateToLocationAndVisibleSize(0, 0, 20000f);
                    }
                }
            }
        });
    }

    private void animateToLocationAndVisibleSize(float longitude, float latitude, float diameter) {
        Log.d(Main.LOG_TAG, String.format("Main.animateAndZoomTo() %.4f, %.4f, %.0fkm", longitude, latitude, diameter));

        mapController.animateTo(longitude, latitude, diameter);
    }


    private void addDataListener(DataListener listener) {
        dataListeners.add(listener);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return persistor;
    }

    public boolean isDebugBuild() {
        boolean dbg = false;

        PackageManager pm = getPackageManager();
        if (pm != null) {

            PackageInfo pi = null;
            try {
                pi = pm.getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            dbg = pi != null && pi.applicationInfo != null && ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
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

        Log.d(Main.LOG_TAG, "Main.onResume()");

        if (dataService != null) {
            dataService.onResume();
        }
        locationHandler.onResume();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (dataService == null || dataService.onPause()) {
            Log.d(Main.LOG_TAG, "Main.onPause() disable location handler");
            locationHandler.onPause();
        } else {
            Log.d(Main.LOG_TAG, "Main.onPause()");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "Main: onDestroy()");

        unbindService(serviceConnection);
    }

    @Override
    public void onBeforeDataUpdate() {
        buttonColumnHandler.disableButtonColumn();
        statusComponent.startProgress();
    }

    @Override
    public void onDataUpdate(DataResult result) {
        Log.d(Main.LOG_TAG, "Main.onDataUpdate() " + result);

        final DataComponent data = result.data();
        final TimeComponent time = result.getTime();
        final SpatialComponent spatial = result.getSpatial();

        statusComponent.indicateError(false);

        historyController.setRealtimeData(time.isRealtime());

        persistor.setCurrentResult(result);

        clearDataIfRequested();

        if (data.getStrokes() != null) {
            if (time.isIncremental()) {
                strokesComponent.expireStrokes();
            } else {
                strokesComponent.clear();
            }

            RasterParameters rasterParameters = spatial.getRasterParameters();
            strokesComponent.setRasterParameters(rasterParameters);
            strokesComponent.setRegion(spatial.getRegion());
            strokesComponent.setReferenceTime(time.getReferenceTime());
            strokesComponent.setIntervalDuration(time.getIntervalDuration());
            strokesComponent.setIntervalOffset(time.getIntervalOffset());

            strokesComponent.addStrokes(data.getStrokes());

            lightningActivityAlarmManager.checkStrokes(strokesComponent.getStrokes(), time.isRealtime());

            ColorHandler colorHandler = strokesComponent.getColorHandler();

            mapController.updateMap(data, time, rasterParameters, colorHandler);

            if (!time.isRealtime()) {
                dataService.disable();
                setHistoricStatusString();
            }

            if (participantsComponent != null && data.getParticipants() != null) {
                participantsComponent.setParticipants(data.getParticipants());
            }

            for (DataListener listener : dataListeners) {
                listener.onDataUpdate(result);
            }

            statusComponent.stopProgress();

            buttonColumnHandler.enableButtonColumn();
        }

        legendView.forceLayout();

        if (dataService != null) {
            dataService.releaseWakeLock();
        }
    }


    private void clearDataIfRequested() {
        if (clearData) {
            clearData = false;

            strokesComponent.clear();

            if (participantsComponent != null) {
                participantsComponent.clear();
            }
        }
    }

    @Override
    public void onDataReset() {
        dataService.reloadData();
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
                dialog = new AlarmDialog(this, lightningActivityAlarmManager, new AlarmDialogColorHandler(PreferenceManager.getDefaultSharedPreferences(this)), strokesComponent.getIntervalDuration());
                break;

            case R.id.settings_dialog:
                dialog = new SettingsDialog(this);
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

            case SHOW_PARTICIPANTS:
                boolean showParticipants = sharedPreferences.getBoolean(key.toString(), true);
                // TODO refresh map
                break;

            case COLOR_SCHEME:
                // TODO refresh map
                break;

            case MAP_FADE:
                int alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.getInt(key.toString(), 40));
                // TODO
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

    public void onDataServiceStatusUpdate(String dataServiceStatus) {
        setStatusString(dataServiceStatus);
    }

    protected void setHistoricStatusString() {
        if (!strokesComponent.hasRealtimeData()) {
            long referenceTime = strokesComponent.getReferenceTime() + strokesComponent.getIntervalOffset() * 60 * 1000;
            String timeString = (String) DateFormat.format("@ kk:mm", referenceTime);
            setStatusString(timeString);
        }
    }

    protected void setStatusString(String runStatus) {
        int numberOfStrokes = strokesComponent.getTotalNumberOfStrokes();
        String statusText = getResources().getQuantityString(R.plurals.stroke, numberOfStrokes, numberOfStrokes);
        statusText += "/";
        int intervalDuration = strokesComponent.getIntervalDuration();
        statusText += getResources().getQuantityString(R.plurals.minute, intervalDuration, intervalDuration);
        statusText += " " + runStatus;

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
            pInfo = getPackageManager() != null ? getPackageManager().getPackageInfo(getPackageName(), 0) : null;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

}