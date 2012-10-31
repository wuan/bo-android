package org.blitzortung.android.data.provider;

import java.util.List;

import android.content.pm.PackageInfo;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.data.beans.Participant;

public abstract class DataProvider {
	
	@SuppressWarnings("unused")
	private static final String TAG = "DataProvider";
	
	protected String username;
	
	protected String password;

    protected PackageInfo pInfo;

	public abstract void setUp();
	
	public abstract void shutDown();
	
	public abstract List<AbstractStroke> getStrokes(int timeInterval, int region);
	
	public abstract List<AbstractStroke> getStrokesRaster(int timeInterval, int params, int timeOffet, int region);
	
	public abstract Raster getRaster();

    public abstract int[] getHistogram();
	
	public abstract List<Participant> getStations(int region);
	
	public abstract DataProviderType getType();
	
	public abstract void reset();
	
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

    public void setPackageInfo(PackageInfo pInfo) {
        this.pInfo = pInfo;
    }
}
