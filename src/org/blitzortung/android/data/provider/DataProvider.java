package org.blitzortung.android.data.provider;

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;

import android.util.Log;

public abstract class DataProvider {
	
	private static final String TAG = "DataProvider";
	
	protected String username;
	
	protected String password;

	abstract public DataResult<Stroke> getStrokes(int timeInterval);
	
	abstract public DataResult<Station> getStations();
	
	abstract public ProviderType getType();
	
	public void setCredentials(String username, String password) {
		Log.v(TAG, String.format("set username '%s'", username));
		this.username = username;
		this.password = password;
	}
}
