package org.blitzortung.android.app;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.app.view.HistogramView;
import org.blitzortung.android.app.view.LegendView;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.DataRetriever;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.dialogs.AlarmDialog;
import org.blitzortung.android.dialogs.InfoDialog;
import org.blitzortung.android.map.OwnMapActivity;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.FadeOverlay;
import org.blitzortung.android.map.overlay.OwnLocationOverlay;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.blitzortung.android.time.RangeHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main extends OwnMapActivity implements DataListener, OnSharedPreferenceChangeListener, TimerTask.TimerUpdateListener,
        AlarmManager.AlarmListener {

    protected TextView status;

    private TextView warning;

    private RangeHandler rangeHandler;

    private ImageButton historyRewind;

    private ImageButton historyForward;

    private ImageButton goRealtime;

    private FadeOverlay fadeOverlay;

    protected StrokesOverlay strokesOverlay;

    private ParticipantsOverlay participantsOverlay;

    private TimerTask timerTask;

    private AlarmManager alarmManager;

    private NotificationManager notificationManager;

    private DataRetriever provider;

    private OwnLocationOverlay ownLocationOverlay;

    private PersistedData persistedData;

    private boolean clearData;

    private float notificationDistanceLimit;

    private float vibrationDistanceLimit;

    private Set<DataListener> dataListeners = new HashSet<DataListener>();

    final Set<String> androidIdsForExtendedFunctionality = new HashSet<String>(Arrays.asList("e73c5a22934b5915"));

    private PackageInfo pInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updatePackageInfo();

        setContentView(isDebugBuild() ? R.layout.main_debug : R.layout.main);

        warning = (TextView) findViewById(R.id.warning);

        status = (TextView) findViewById(R.id.status);

        setMapView((OwnMapView) findViewById(R.id.mapview));

        getMapView().setBuiltInZoomControls(true);

        final String androidId = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (isDebugBuild() || (androidId != null && androidIdsForExtendedFunctionality.contains(androidId))) {
            ImageButton rasterToggle = (ImageButton) findViewById(R.id.toggleExtendedMode);
            rasterToggle.setEnabled(true);
            rasterToggle.setVisibility(View.VISIBLE);
            rasterToggle.getDrawable().setAlpha(40);
            rasterToggle.getBackground().setAlpha(40);

            rasterToggle.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    provider.toggleExtendedMode();
                    onDataReset();
                }
            });
        }

        historyRewind = (ImageButton) findViewById(R.id.historyRew);
        historyRewind.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (provider.rewInterval()) {
                    onDataReset();
                    historyForward.setVisibility(View.VISIBLE);
                    goRealtime.setVisibility(View.VISIBLE);
                    DataRetriever.UpdateTargets updateTargets = new DataRetriever.UpdateTargets();
                    updateTargets.updateStrokes();
                    provider.updateData(updateTargets);
                } else {
                    Toast toast = Toast.makeText(getBaseContext(), getResources().getText(R.string.historic_timestep_limit_reached), 1000);
                    toast.show();
                }
            }
        });

        historyForward = (ImageButton) findViewById(R.id.historyFfwd);
        historyForward.setVisibility(View.INVISIBLE);
        historyForward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (provider.ffwdInterval()) {
                    onDataReset();
                    if (provider.isRealtime()) {
                        historyForward.setVisibility(View.INVISIBLE);
                        goRealtime.setVisibility(View.INVISIBLE);
                    }
                    DataRetriever.UpdateTargets updateTargets = new DataRetriever.UpdateTargets();
                    updateTargets.updateStrokes();
                    provider.updateData(updateTargets);
                }
            }
        });

        goRealtime = (ImageButton) findViewById(R.id.goRealtime);
        goRealtime.setVisibility(View.INVISIBLE);
        goRealtime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (provider.goRealtime()) {
                    onDataReset();
                    historyForward.setVisibility(View.INVISIBLE);
                    goRealtime.setVisibility(View.INVISIBLE);
                    onResume();
                }
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (getLastNonConfigurationInstance() == null) {
            persistedData = new PersistedData(getResources(), (LocationManager) getSystemService(Context.LOCATION_SERVICE), preferences, pInfo);
        } else {
            persistedData = (PersistedData) getLastNonConfigurationInstance();
        }

        strokesOverlay = persistedData.getStrokesOverlay();
        strokesOverlay.setActivity(this);

        participantsOverlay = persistedData.getParticipantsOverlay();
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

        provider = persistedData.getProvider();
        provider.setDataListener(this);

        int historyButtonsVisibility = strokesOverlay.hasRealtimeData() ? View.INVISIBLE : View.VISIBLE;
        historyForward.setVisibility(historyButtonsVisibility);
        goRealtime.setVisibility(historyButtonsVisibility);
        setHistoricStatusString();

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        provider.setProgressBar(progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        ImageView errorIndicator = (ImageView) findViewById(R.id.error_indicator);
        errorIndicator.setVisibility(View.INVISIBLE);
        provider.setErrorIndicator(errorIndicator);

        timerTask = persistedData.getTimerTask();
        timerTask.setListener(this);

        alarmManager = persistedData.getAlarmManager();
        alarmManager.clearAlarmListeners();
        alarmManager.addAlarmListener(this);
        if (alarmManager.isAlarmEnabled()) {
            onAlarmResult(alarmManager.getAlarmStatus());
        }
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ownLocationOverlay = new OwnLocationOverlay(getBaseContext(), getMapView());

        /*ImageButton toggleAnimation = (ImageButton) findViewById(R.id.toggleAnimation);
        toggleAnimation.setEnabled(true);
        toggleAnimation.setVisibility(View.VISIBLE);
        toggleAnimation.getDrawable().setAlpha(200);

        toggleAnimation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                provider.toggleAnimation();
                onDataReset();
            }
        });*/

        LegendView legendView = (LegendView) findViewById(R.id.legend_view);
        legendView.setStrokesOverlay(strokesOverlay);
        legendView.setAlpha(150);

        AlarmView alarmView = (AlarmView) findViewById(R.id.alarm_view);
        alarmView.setAlarmManager(alarmManager);
        alarmView.setColorHandler(strokesOverlay.getColorHandler(), strokesOverlay.getIntervalDuration());
        alarmView.setBackgroundColor(Color.TRANSPARENT);
        alarmView.setAlpha(200);

        HistogramView histogramView = (HistogramView) findViewById(R.id.histogram_view);
        histogramView.setStrokesOverlay(strokesOverlay);
        if (persistedData.getCurrentResult() != null) {
            histogramView.onDataUpdate(persistedData.getCurrentResult());
        }
        addDataListener(histogramView);

        onSharedPreferenceChanged(preferences, Preferences.MAP_TYPE_KEY);
        onSharedPreferenceChanged(preferences, Preferences.MAP_FADE_KEY);
        onSharedPreferenceChanged(preferences, Preferences.SHOW_LOCATION_KEY);
        onSharedPreferenceChanged(preferences, Preferences.NOTIFICATION_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, Preferences.VIBRATION_DISTANCE_LIMIT);
        onSharedPreferenceChanged(preferences, Preferences.DO_NOT_SLEEP);

        getMapView().invalidate();
    }

    private void addDataListener(DataListener listener) {
        dataListeners.add(listener);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        strokesOverlay.clearPopup();
        participantsOverlay.clearPopup();
        return persistedData;
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
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
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

        timerTask.onResume(provider.isRealtime());

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
    public void onDataUpdate(DataResult result) {
        persistedData.setCurrentResult(result);

        clearDataIfRequested();

        if (result.containsStrokes()) {
            strokesOverlay.setRaster(result.getRaster());
            strokesOverlay.setRegion(result.getRegion());
            strokesOverlay.setReferenceTime(result.getReferenceTime());
            strokesOverlay.setIntervalDuration(result.getIntervalDuration());
            strokesOverlay.setIntervalOffset(result.getIntervalOffset());
            strokesOverlay.addStrokes(result.getStrokes());

            if (alarmManager.isAlarmEnabled()) {
                alarmManager.check(result);
            }
            strokesOverlay.refresh();
        }

        if (result.containsRealtimeData()) {
            timerTask.enable();
        } else {
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

        if (strokesOverlay.isRaster()) {
            int region = strokesOverlay.getRegion();
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
                sendNotification(getResources().getString(R.string.activity) + ": " + alarmStatus.getTextMessage(notificationDistanceLimit));
            } else {
                clearNotification();
            }
        } else {
            clearNotification();
        }
    }

    @Override
    public void onAlarmClear() {
        warning.setText("");
    }

    private void sendNotification(String notificationText) {
        if (notificationManager != null) {
            Notification notification = new Notification(R.drawable.icon, notificationText, System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, Main.class), 0);
            notification.setLatestEventInfo(this, getResources().getText(R.string.app_name), notificationText, contentIntent);

            notificationManager.notify(R.id.alarm_notification_id, notification);
        }
    }

    private void clearNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(R.id.alarm_notification_id);
        }
    }

    private void updatePackageInfo() {
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}