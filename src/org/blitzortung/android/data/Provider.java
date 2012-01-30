package org.blitzortung.android.data;

import java.util.List;

import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.JsonRpcProvider;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

public class Provider {

	private DataProvider dataProvider;

	private ProgressBar progress;

	private DataListener listener;

	private class RetrieveStrokesTask extends AsyncTask<Integer, Integer, List<Stroke>> {

		protected void onProgressUpdate(Integer... progress) {
			Log.v("RetrieveStrokesTask", String.format("update progress %d"));
		}

		protected void onPostExecute(List<Stroke> strokes) {
			listener.onStrokeDataArrival(strokes);

			progress.setVisibility(View.INVISIBLE);
			progress.setProgress(progress.getMax());
		}

		@Override
		protected List<Stroke> doInBackground(Integer... params) {
			return dataProvider.getStrokes();
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

		new RetrieveStrokesTask().execute(-60);
	}
}
