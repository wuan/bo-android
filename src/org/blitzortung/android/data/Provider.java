package org.blitzortung.android.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.data.provider.JsonRpcProvider;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class Provider implements OnSharedPreferenceChangeListener {

	private static final String TAG = "Provider";

	private final Lock lock = new ReentrantLock();

	private DataProvider dataProvider;

	private String username;
	private String password;

	private ProgressBar progress;
	private ImageView error_indicator;

	private DataListener listener;

	public Provider(SharedPreferences sharedPreferences, ProgressBar progress, ImageView error_indicator, DataListener listener) {
		dataProvider = new JsonRpcProvider();
		this.progress = progress;
		this.error_indicator = error_indicator;
		this.listener = listener;

		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		
		onSharedPreferenceChanged(sharedPreferences, Preferences.USERNAME_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.PASSWORD_KEY);
		onSharedPreferenceChanged(sharedPreferences, Preferences.RASTER_SIZE_KEY);

		progress.setVisibility(View.INVISIBLE);
		error_indicator.setVisibility(View.INVISIBLE);
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
				progress.setVisibility(View.INVISIBLE);
				progress.setProgress(progress.getMax());
				
				error_indicator.setVisibility(result.hasFailed() ? View.VISIBLE : View.INVISIBLE);				
			}
		}

		@Override
		protected DataResult doInBackground(Integer... params) {
			DataResult result = new DataResult();

			if (lock.tryLock()) {
				try {
					dataProvider.setUp();
					List<AbstractStroke> strokes = new ArrayList<AbstractStroke>();
					if (params[1] == 0) {
						strokes = dataProvider.getStrokes(params[0]);
					} else {
						strokes = dataProvider.getStrokesRaster(params[0], params[1]);
					}
					result.setStrokes(strokes);
					result.setRaster(dataProvider.getRaster());
					
					if (params.length > 2 && params[2] != 0)
					  result.setStations(dataProvider.getStations());
					dataProvider.shutDown();
				} catch (RuntimeException e) {
					e.printStackTrace();
					// handle silently
				} finally {
					lock.unlock();
				}
			} else {
				result.setProcessWasLocked();
				Log.v("Provider", "could not get lock on update task");
			}
			return result;
		}
	}

	public void updateData(int minutes, int updateStations) {
		progress.setVisibility(View.VISIBLE);
		progress.setProgress(0);

		new FetchDataTask().execute(minutes, rasterSize);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.USERNAME_KEY)) {
			username = sharedPreferences.getString(Preferences.USERNAME_KEY, "");
			Log.v(TAG, String.format("update %s to %s", key, username));
		} else if (key.equals(Preferences.PASSWORD_KEY)) {
			password = sharedPreferences.getString(Preferences.PASSWORD_KEY, "");
			Log.v(TAG, String.format("update %s to *****", key));
		} else if (key.equals(Preferences.RASTER_SIZE_KEY)) {
			rasterSize = Integer.parseInt(sharedPreferences.getString(Preferences.RASTER_SIZE_KEY, "10000"));
		}

		if (dataProvider != null) {
			dataProvider.setCredentials(username, password);
		}
	}

	int rasterSize;
	
	/*public void toggleRaster() {
		raster = !raster;
	}*/
}
