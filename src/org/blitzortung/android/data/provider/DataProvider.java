package org.blitzortung.android.data.provider;

import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.data.beans.Station;

public abstract class DataProvider {
	
	@SuppressWarnings("unused")
	private static final String TAG = "DataProvider";
	
	protected String username;
	
	protected String password;

	abstract public void setUp();
	
	abstract public void shutDown();
	
	abstract public List<AbstractStroke> getStrokes(int timeInterval);
	
	abstract public List<AbstractStroke> getStrokesRaster(int timeInterval, int params, int timeOffet, int region);
	
	abstract public Raster getRaster();
	
	abstract public List<Station> getStations();
	
	abstract public ProviderType getType();
	
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}
}
