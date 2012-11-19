package org.blitzortung.android.app;

import java.util.*;

import android.graphics.Color;
import android.view.*;
import android.widget.*;
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
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.DimOverlay;
import org.blitzortung.android.map.overlay.OwnLocationOverlay;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;

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
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

import com.google.android.maps.Overlay;

public class Main extends OwnMapActivity implements DataListener, OnSharedPreferenceChangeListener, TimerTask.StatusListener,
		AlarmManager.AlarmListener {

	private TextView status;

	private TextView warning;

	private StrokesOverlay strokesOverlay;

	private ParticipantsOverlay participantsOverlay;

	private TimerTask timerTask;

	private AlarmManager alarmManager;

	private NotificationManager notificationManager;

	private DataRetriever provider;

	private OwnLocationOverlay ownLocationOverlay;

	private PersistedData persistedData;

	private boolean resetData;

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

		final String androidId = Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID);
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

        mapOverlays.add(new DimOverlay(strokesOverlay.getColorHandler()));
		mapOverlays.add(strokesOverlay);
		mapOverlays.add(participantsOverlay);

		provider = persistedData.getProvider();
		provider.setDataListener(this);

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
        alarmView.setColorHandler(strokesOverlay.getColorHandler(), strokesOverlay.getMinutesPerColor());
        alarmView.setBackgroundColor(Color.TRANSPARENT);
        alarmView.setAlpha(200);

        HistogramView histogramView = (HistogramView) findViewById(R.id.histogram_view);
        histogramView.setStrokesOverlay(strokesOverlay);
        if (persistedData.getCurrentResult() != null) {
            histogramView.onDataUpdate(persistedData.getCurrentResult());
        }
        addDataListener(histogramView);

		onSharedPreferenceChanged(preferences, Preferences.MAP_TYPE_KEY);
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

	public void onStatusUpdate(String statusText) {
		status.setText(statusText);
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

		timerTask.onResume();

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

		if (resetData) {
			resetData = false;
			strokesOverlay.clear();

			if (participantsOverlay != null) {
				participantsOverlay.clear();
			}
		}

		if (result.containsStrokes()) {
			long expireTime = System.currentTimeMillis() - provider.getMinutes() * 60 * 1000;

			strokesOverlay.setRaster(result.getRaster());
			strokesOverlay.addAndExpireStrokes(result.getStrokes(), expireTime);

			timerTask.setNumberOfStrokes(strokesOverlay.getTotalNumberOfStrokes());

			if (alarmManager.isAlarmEnabled()) {
				alarmManager.check(result);
			}
			strokesOverlay.refresh();
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

	@Override
	public void onDataReset() {
		timerTask.restart();
		resetData = true;

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
			dialog = new AlarmDialog(this, alarmManager, strokesOverlay.getColorHandler(), strokesOverlay.getMinutesPerColor());
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