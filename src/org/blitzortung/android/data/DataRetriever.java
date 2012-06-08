package org.blitzortung.android.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.data.provider.DataProviderType;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class DataRetriever implements OnSharedPreferenceChangeListener {

	private static final String TAG = "DataRetriever";

	private final Lock lock = new ReentrantLock();

	private DataProvider dataProvider;

	private String username;
	private String password;

	private ProgressBar progressBar;
	private ImageView errorIndicator;

	private int minutes = 60;

	private int minuteOffset = 0;

	private int region = 1;

	private int rasterSize;

	private DataListener listener;

	public static class UpdateTargets {

		boolean updateStrokes;

		boolean updateStations;

		public UpdateTargets() {
			updateStrokes = false;
			updateStations = false;
		}

		public void addStrokes() {
			updateStrokes = true;
		}

		public boolean updateStrokes() {
			return updateStrokes;
		}

		public void addStations() {
			updateStations = true;
		}

		public boolean updateStations() {
			return updateStations;
		}

		public boolean anyUpdateRequested() {
			return updateStrokes || updateStations;
		}
	};

	public DataRetriever(SharedPreferences sharedPreferences) {
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);

		onSharedPreferenceChanged(sharedPreferences, Preferences.DATA_SOURCE_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.USERNAME_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.PASSWORD_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.RASTER_SIZE_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.REGION_KEY);
	}

	private class FetchDataTask extends AsyncTask<Integer, Integer, DataResult> {

		protected void onProgressUpdate(Integer... progress) {
			Log.v("RetrieveStrokesTask", String.format("update progress %d", progress[0]));
		}

		protected void onPostExecute(DataResult result) {
			if (!result.hasFailed()) {
				listener.onDataUpdate(result);
			}

			if (!result.processWasLocked()) {
				progressBar.setVisibility(View.INVISIBLE);
				progressBar.setProgress(progressBar.getMax());

				errorIndicator.setVisibility(result.hasFailed() ? View.VISIBLE : View.INVISIBLE);
			}
		}

		@Override
		protected DataResult doInBackground(Integer... params) {
			DataResult result = new DataResult();

			if (lock.tryLock()) {
				try {
					dataProvider.setUp();
					dataProvider.setCredentials(username, password);
					
					List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();
					if (params[1] == 0) {
						result.setIncremental();
						strokes = dataProvider.getStrokes(params[0], params[3]);
					} else {
						strokes = dataProvider.getStrokesRaster(params[0], params[1], params[2], params[3]);
					}
					result.setStrokes(strokes);
					result.setRaster(dataProvider.getRaster());

					if (params.length > 4 && params[4] != 0)
						result.setStations(dataProvider.getStations());
					dataProvider.shutDown();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			} else {
				result.setProcessWasLocked();
				Log.v(TAG, "could not get lock on update task");
			}
			return result;
		}
	}

	public void updateData(UpdateTargets updateTargets) {
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setProgress(0);

		new FetchDataTask().execute(minutes, dataProvider.getType() == DataProviderType.HTTP ? 0 : rasterSize, minuteOffset, region, updateTargets.updateStations() ? 1 : 0);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.DATA_SOURCE_KEY)) {
			String providerTypeString = sharedPreferences.getString(Preferences.DATA_SOURCE_KEY, DataProviderType.RPC.toString());
			DataProviderType providerType = DataProviderType.valueOf(providerTypeString.toUpperCase());
			Log.v(TAG, String.format("update %s to %s", key, providerType.toString()));
			dataProvider = providerType.getProvider();
			if (listener != null) {
				listener.onDataReset();
			}
		} else if (key.equals(Preferences.USERNAME_KEY)) {
			username = sharedPreferences.getString(Preferences.USERNAME_KEY, "");
			Log.v(TAG, String.format("update %s to %s", key, username));
		} else if (key.equals(Preferences.PASSWORD_KEY)) {
			password = sharedPreferences.getString(Preferences.PASSWORD_KEY, "");
			Log.v(TAG, String.format("update %s to *****", key));
		} else if (key.equals(Preferences.RASTER_SIZE_KEY)) {
			rasterSize = Integer.parseInt(sharedPreferences.getString(Preferences.RASTER_SIZE_KEY, "10000"));
		} else if (key.equals(Preferences.REGION_KEY)) {
			region = Integer.parseInt(sharedPreferences.getString(Preferences.REGION_KEY, "1"));
			dataProvider.reset();
			if (listener != null) {
				listener.onDataReset();
			}
		}

		if (dataProvider != null) {
			dataProvider.setCredentials(username, password);
		}
	}

	public int getMinutes() {
		return minutes;
	}

	public int getRegion() {
		return region;
	}

	public boolean isUsingRaster() {
		return rasterSize != 0;
	}

	private static int storedRasterSize;

	public void toggleRaster() {
		if (rasterSize > 0) {
			storedRasterSize = rasterSize;
			rasterSize = 0;
		} else {
			rasterSize = storedRasterSize;
		}
	}

	public void setProgressBar(ProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	public void setErrorIndicator(ImageView errorIndicator) {
		this.errorIndicator = errorIndicator;
	}

	public void setDataListener(DataListener listener) {
		this.listener = listener;
	}

}
