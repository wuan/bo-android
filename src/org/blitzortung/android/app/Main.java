package org.blitzortung.android.app;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.alarm.AlarmLabel;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmStatus;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.DataRetriever;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.dialogs.AlarmDialog;
import org.blitzortung.android.dialogs.InfoDialog;
import org.blitzortung.android.dialogs.LegendDialog;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.OwnLocationOverlay;
import org.blitzortung.android.map.overlay.StationsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;

import android.app.Dialog;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.maps.Overlay;

public class Main extends OwnMapActivity implements DataListener, OnSharedPreferenceChangeListener, TimerTask.StatusListener,
		AlarmManager.AlarmListener {

	private static final String TAG = "Main";

	TextView status;

	TextView warning;

	StrokesOverlay strokesOverlay;

	StationsOverlay stationsOverlay;

	private TimerTask timerTask;

	private AlarmManager alarmManager;

	private DataRetriever provider;

	OwnLocationOverlay ownLocationOverlay;

	private PersistedData persistedData;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v("Main", "onCreate()");

		setContentView(isDebugBuild() ? R.layout.main_debug : R.layout.main);

		warning = (TextView) findViewById(R.id.warning);

		status = (TextView) findViewById(R.id.status);


		setMapView((OwnMapView) findViewById(R.id.mapview));

		getMapView().setBuiltInZoomControls(true);

		final String androidId = Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID);

		if (isDebugBuild() || androidId.equals("e73c5a22934b5915")) {
			RelativeLayout mapcontainer = (RelativeLayout) findViewById(R.id.mapcontainer);

			Button rasterToggle = new Button(getBaseContext());
			rasterToggle.setText("r/p");

			rasterToggle.getBackground().setAlpha(150);

			rasterToggle.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					provider.toggleRaster();
					strokesOverlay.clear();
					timerTask.restart();
				}
			});

			mapcontainer.addView(rasterToggle);
		}

		ownLocationOverlay = new OwnLocationOverlay(getBaseContext(), getMapView());

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);

		if (getLastNonConfigurationInstance() == null) {
			persistedData = new PersistedData(getResources(), (LocationManager)getSystemService(Context.LOCATION_SERVICE), preferences);
		} else {
			persistedData = (PersistedData) getLastNonConfigurationInstance();
		}

		strokesOverlay = persistedData.getStrokesOverlay();
		strokesOverlay.setActivity(this);

		// stationsOverlay = new StationsOverlay(this, new StationColorHandler(preferences));

		getMapView().addZoomListener(new OwnMapView.ZoomListener() {

			@Override
			public void onZoom(int zoomLevel) {
				strokesOverlay.updateZoomLevel(zoomLevel);
				// stationsOverlay.updateShapeSize(zoomLevel);
			}

		});

		List<Overlay> mapOverlays = getMapView().getOverlays();

		mapOverlays.add(strokesOverlay);
		// mapOverlays.add(stationsOverlay);

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

		onSharedPreferenceChanged(preferences, Preferences.MAP_TYPE_KEY);
		onSharedPreferenceChanged(preferences, Preferences.SHOW_LOCATION_KEY);

		getMapView().invalidate();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		Log.v("Main", "onRetainNonConfigurationInstance()");
		strokesOverlay.clearPopup();
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
		} catch (Exception e) {
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

		case R.id.menu_legend:
			showDialog(R.id.legend_dialog);
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
		Log.v(TAG, "onResume()");
		timerTask.onResume();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getBoolean(Preferences.SHOW_LOCATION_KEY, false)) {
			ownLocationOverlay.enableOwnLocation();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.v(TAG, "onPause()");
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
		if (result.containsStrokes()) {
			Calendar expireTime = new GregorianCalendar();

			expireTime.add(Calendar.MINUTE, -provider.getMinutes());

			strokesOverlay.setRaster(result.getRaster());
			strokesOverlay.addAndExpireStrokes(result.getStrokes(), expireTime.getTime().getTime());

			timerTask.setNumberOfStrokes(strokesOverlay.getTotalNumberOfStrokes());

			if (alarmManager.isAlarmEnabled()) {
				alarmManager.check(result);
			}
			strokesOverlay.refresh();
		}

		if (stationsOverlay != null && result.containsStations()) {
			stationsOverlay.setStations(result.getStations());
			stationsOverlay.refresh();
		}

		getMapView().invalidate();
	}

	@Override
	public void onDataReset() {
		strokesOverlay.clear();
		timerTask.restart();
		strokesOverlay.refresh();
		if (stationsOverlay != null) {
			stationsOverlay.clear();
			stationsOverlay.refresh();
		}
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case R.id.info_dialog:
			dialog = new InfoDialog(this);
			break;

		case R.id.legend_dialog:
			dialog = new LegendDialog(this, strokesOverlay);
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
			if (stationsOverlay != null) {
				stationsOverlay.refresh();
			}
		} else if (key.equals(Preferences.RASTER_SIZE_KEY)) {
			timerTask.restart();
		} else if (key.equals(Preferences.REGION_KEY)) {
			timerTask.restart();
		} else if (key.equals(Preferences.SHOW_LOCATION_KEY)) {
			boolean showLocation = sharedPreferences.getBoolean(Preferences.SHOW_LOCATION_KEY, false);

			if (showLocation) {
				ownLocationOverlay.enableOwnLocation();
			} else {
				ownLocationOverlay.disableOwnLocation();
			}
		}
	}

	@Override
	public void onAlarmResult(AlarmStatus alarmStatus) {
		AlarmLabel alarmLabel = new AlarmLabel(warning, getResources());

		alarmLabel.apply(alarmStatus);

		if (alarmStatus != null && alarmStatus.getClosestStrokeDistance() < 50.0) {
			Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(40);
		}
	}

	@Override
	public void onAlarmClear() {
		warning.setText("");
	}

}