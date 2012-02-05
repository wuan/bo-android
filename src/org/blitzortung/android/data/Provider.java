package org.blitzortung.android.data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.data.provider.JsonRpcProvider;
import org.blitzortung.android.data.provider.ProviderType;

import android.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

public class Provider implements OnSharedPreferenceChangeListener {

	private static final String TAG = "Provider";

	private final Lock lock = new ReentrantLock();

	private static final String DATA_SOURCE_PREFS_KEY = "data_source";
	private static final String USERNAME_PREFS_KEY = "data_source";
	private static final String PASSWORD_PREFS_KEY = "data_source";
	
	private DataProvider dataProvider;
	
	private String username;
	private String password;

	private ProgressBar progress;

	private DataListener listener;

	public Provider(SharedPreferences sharedPreferences, ProgressBar progress, DataListener listener) {
		dataProvider = new JsonRpcProvider();
		this.progress = progress;
		this.listener = listener;

		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(sharedPreferences, DATA_SOURCE_PREFS_KEY);

		progress.setVisibility(View.INVISIBLE);
	}

	private class RetrieveStrokesTask extends AsyncTask<Integer, Integer, DataResult<Stroke>> {

		protected void onProgressUpdate(Integer... progress) {
			Log.v("RetrieveStrokesTask", String.format("update progress %d", progress[0]));
		}

		protected void onPostExecute(DataResult<Stroke> strokes) {
			if (strokes.retrievalWasSuccessful()) {
				listener.onStrokeDataArrival(strokes.getData());
			}

			if (!strokes.processWasLocked()) {
				progress.setVisibility(View.INVISIBLE);
				progress.setProgress(progress.getMax());
			}
		}

		@Override
		protected DataResult<Stroke> doInBackground(Integer... params) {
			DataResult<Stroke> strokes = new DataResult<Stroke>();

			if (lock.tryLock()) {
				try {
					Log.v("RetrieveStrokesTask", String.format("doInBackground(%d)", params[0]));
					strokes = dataProvider.getStrokes(params[0]);
				} finally {
					lock.unlock();
				}
			} else {
				strokes.setProcessWasLocked();
				Log.v("Provider", "could not get lock on update task");
			}
			return strokes;
		}
	}

	public void updateStrokes() {
		progress.setVisibility(View.VISIBLE);
		progress.setProgress(0);

		new RetrieveStrokesTask().execute(60);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(DATA_SOURCE_PREFS_KEY)) {
			String providerTypeString = sharedPreferences.getString(DATA_SOURCE_PREFS_KEY, ProviderType.HTTP.toString());
			ProviderType providerType = ProviderType.valueOf(providerTypeString);
			Log.v(TAG, String.format("update %s to %s", key, providerType.toString()));
			dataProvider = providerType.getProvider();

		} else if (key.equals(USERNAME_PREFS_KEY)) {
			username = sharedPreferences.getString(USERNAME_PREFS_KEY, "");
		} else if (key.equals(PASSWORD_PREFS_KEY)) {
			password = sharedPreferences.getString(USERNAME_PREFS_KEY, "");
		}
		dataProvider.setCredentials(username, password);
	}
}
