package org.blitzortung.android.app;

import java.util.Calendar;

import org.blitzortung.android.data.Provider;

import android.content.res.Resources;
import android.os.Handler;

class TimerTask implements Runnable {

	int period = 60;
	long nextUpdate = 0;
	int stationPeriod = 10 * 60;
	long nextStationUpdate = 0;
	
	int minutes = 60;
	
	int numberOfStrokes = 0;
	
	private Main app;
	
	private Provider provider;
	
	private Handler handler;
	
	TimerTask(Main app, Provider provider) {
		this.app = app;
		this.provider = provider;
		handler = new Handler();
	}

	@Override
	public void run() {
		long now = Calendar.getInstance().getTimeInMillis() / 1000;

		if (now >= nextUpdate) {
			int updateStations = 0;

			/*if (stationsOverlay != null && now >= nextStationUpdate) {
				updateStations = 1;
				nextStationUpdate = now + stationPeriod;
			}*/
			provider.updateData(minutes, updateStations);
			nextUpdate = now + period;
		}
		
		Resources res = app.getResources();
		String statusString = res.getQuantityString(R.plurals.stroke, numberOfStrokes, numberOfStrokes);
		statusString += "/";
		statusString += res.getQuantityString(R.plurals.minute, minutes, minutes);
		statusString += String.format(", %d/%ds", nextUpdate - now, period);
		app.setStatusText(statusString);

		// Schedule the next update in one second
		handler.postDelayed(this, 1000);
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public void reset() {
		nextUpdate = 0;
		nextStationUpdate = 0;
	}

	public void onResume() {
		handler.post(this);		
	}

	public void onPause() {
		handler.removeCallbacks(this);		
	}

};

