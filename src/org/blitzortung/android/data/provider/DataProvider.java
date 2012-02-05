package org.blitzortung.android.data.provider;

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;

public abstract class DataProvider {
	
	private String username;
	
	private String password;

	abstract public DataResult<Stroke> getStrokes(int timeInterval);
	
	abstract public DataResult<Station> getStations();
	
	abstract public ProviderType getType();
	
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}
}
