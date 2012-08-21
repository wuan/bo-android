package org.blitzortung.android.data;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.DataProviderType;
import org.blitzortung.android.data.provider.DataResult;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataRetriever implements OnSharedPreferenceChangeListener {

	private final Lock lock = new ReentrantLock();

	private DataProvider dataProvider;

	private String username;
	private String password;

	private ProgressBar progressBar;
	private ImageView errorIndicator;

	private final int minutes = 60;

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

		public void addParticipants() {
			updateStations = true;
		}

		public boolean updateParticipants() {
			return updateStations;
		}

		public boolean anyUpdateRequested() {
			return updateStrokes || updateStations;
		}
	}

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
					
					List<AbstractStroke> strokes;
					if (params[1] == 0) {
						result.setIncremental();
						strokes = dataProvider.getStrokes(params[0], params[3]);
					} else {
						strokes = dataProvider.getStrokesRaster(params[0], params[1], params[2], params[3]);
					}
                    result.setStrokesTimeInterval(minutes * 60 * 1000);
					result.setStrokes(strokes);
					result.setRaster(dataProvider.getRaster());

					if (params.length > 4 && params[4] != 0)
						result.setParticipants(dataProvider.getStations(params[3]));
					dataProvider.shutDown();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			} else {
				result.setProcessWasLocked();
			}
			return result;
		}
	}

	public void updateData(UpdateTargets updateTargets) {
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setProgress(0);
		
		boolean updateParticipants = false;
		if (updateTargets.updateParticipants()) {
			if (dataProvider.getType() == DataProviderType.HTTP || rasterSize == 0) {
				updateParticipants = true;
			}
		}

        int minuteOffset = 0;
        new FetchDataTask().execute(minutes, dataProvider.getType() == DataProviderType.HTTP ? 0 : rasterSize, minuteOffset, region, updateParticipants ? 1 : 0);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.DATA_SOURCE_KEY)) {
			String providerTypeString = sharedPreferences.getString(Preferences.DATA_SOURCE_KEY, DataProviderType.RPC.toString());
			DataProviderType providerType = DataProviderType.valueOf(providerTypeString.toUpperCase());
			dataProvider = providerType.getProvider();
			if (listener != null) {
				listener.onDataReset();
			}
		} else if (key.equals(Preferences.USERNAME_KEY)) {
			username = sharedPreferences.getString(Preferences.USERNAME_KEY, "");
		} else if (key.equals(Preferences.PASSWORD_KEY)) {
			password = sharedPreferences.getString(Preferences.PASSWORD_KEY, "");
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

	public void toggleExtendedMode() {
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
