package org.blitzortung.android.app;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.Provider;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.map.StrokesMapView;
import org.blitzortung.android.map.overlay.StationsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;

public class Main extends MapActivity implements LocationListener, DataListener, OnSharedPreferenceChangeListener {

	private static final String TAG = "Main";

	private final static String MAP_TYPE_PREFS_KEY = "map_mode";

	Location presentLocation;

	StrokesMapView mapView;

	TextView statusText;

	Provider provider;

	StrokesOverlay strokesOverlay;
	
	StationsOverlay stationsOverlay;

	int numberOfStrokes = 0;

	int minutes = 60;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);

		mapView = (StrokesMapView) findViewById(R.id.mapview);

		mapView.setBuiltInZoomControls(true);

		statusText = (TextView) findViewById(R.id.status);

		strokesOverlay = new StrokesOverlay();
		
		stationsOverlay = new StationsOverlay();

		provider = new Provider(preferences, (ProgressBar) findViewById(R.id.progress), (ImageView) findViewById(R.id.error_indicator),
				this);

		mapView.addZoomListener(new StrokesMapView.ZoomListener() {

			@Override
			public void onZoom(int zoomLevel) {
				strokesOverlay.updateShapeSize(zoomLevel);
				stationsOverlay.updateShapeSize(zoomLevel);
			}

		});

		List<Overlay> mapOverlays = mapView.getOverlays();
		mapOverlays.add(strokesOverlay);
		mapOverlays.add(stationsOverlay);

		onSharedPreferenceChanged(preferences, MAP_TYPE_PREFS_KEY);

		mapView.invalidate();
	}

	private Handler mHandler = new Handler();

	private TimerTask timerTask = new TimerTask();

	class TimerTask implements Runnable {

		int period = 20;
		long nextUpdate = 0;
		int stationPeriod = 10 * 60;
		long nextStationUpdate = 0;


		@Override
		public void run() {
			long now = Calendar.getInstance().getTimeInMillis() / 1000;

			if (now >= nextUpdate) {
				int updateStations = 0;
				
				if (now >= nextStationUpdate) {
					updateStations = 1;
					nextStationUpdate = now + stationPeriod;
				}
				provider.updateData(minutes, updateStations);
				nextUpdate = now + period;
			}

			statusText.setText(String.format("%d strokes/%d minutes, %d/%ds", numberOfStrokes, minutes,
					nextUpdate - now, period));

			// Schedule the next update in one second
			mHandler.postDelayed(timerTask, 1000);
		}

		public void setPeriod(int period) {
			this.period = period;
		}

		public void reset() {
			nextUpdate = 0;
		}
	};

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
			showDialog(DIALOG_INFO_ID);
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
		mHandler.post(timerTask);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.v(TAG, "onPause()");
		mHandler.removeCallbacks(timerTask);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {
		presentLocation = location;
		Log.v(TAG, "New location received");
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDataUpdate(DataResult result) {
		if (result.containsStrokes()) {
			strokesOverlay.addStrokes(result.getStrokes());

			Calendar expireTime = new GregorianCalendar();
			expireTime.add(Calendar.MINUTE, -minutes);
			strokesOverlay.expireStrokes(expireTime.getTime());

			numberOfStrokes = strokesOverlay.size();

			strokesOverlay.refresh();
		}

		if (result.containsStations()) {
			stationsOverlay.setStations(result.getStations());
			stationsOverlay.refresh();
		}
		
		mapView.invalidate();
	}

	@Override
	public void onDataReset() {
		strokesOverlay.clear();
		stationsOverlay.clear();
		timerTask.reset();
		strokesOverlay.refresh();
	}

	static final int DIALOG_INFO_ID = 0;

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_INFO_ID:
			dialog = new InfoDialog(this);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(MAP_TYPE_PREFS_KEY)) {
			String mapTypeString = sharedPreferences.getString(MAP_TYPE_PREFS_KEY, "SATELLITE");
			mapView.setSatellite(mapTypeString.equals("SATELLITE"));
		}
	}

}