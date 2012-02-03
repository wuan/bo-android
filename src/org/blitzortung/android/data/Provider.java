package org.blitzortung.android.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.JsonRpcProvider;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

public class Provider {

	private final Lock lock = new ReentrantLock();

	private DataProvider dataProvider;

	private ProgressBar progress;

	private DataListener listener;

	private class RetrieveStrokesTask extends AsyncTask<Integer, Integer, List<Stroke>> {

		protected void onProgressUpdate(Integer... progress) {
			Log.v("RetrieveStrokesTask", String.format("update progress %d", progress[0]));
		}

		protected void onPostExecute(List<Stroke> strokes) {
			if (strokes != null) {
				listener.onStrokeDataArrival(strokes);

				progress.setVisibility(View.INVISIBLE);
				progress.setProgress(progress.getMax());
			}
		}

		@Override
		protected List<Stroke> doInBackground(Integer... params) {
			List<Stroke> strokes = new ArrayList<Stroke>();

			if (lock.tryLock()) {
				try {
					Log.v("RetrieveStrokesTask", String.format("doInBackground(%d)", params[0]));
					strokes = dataProvider.getStrokes(params[0]);
				} finally {
					lock.unlock();
				}
			} else {
				Log.v("Provider", "could not get lock on update task");
			}
			return strokes;
		}
	}

	public Provider(Credentials creds, ProgressBar progress, DataListener listener) {
		dataProvider = new JsonRpcProvider(creds);
		this.progress = progress;
		this.listener = listener;

		progress.setVisibility(View.INVISIBLE);
	}

	public void updateStrokes() {
		progress.setVisibility(View.VISIBLE);
		progress.setProgress(0);

		new RetrieveStrokesTask().execute(60);
	}
}
