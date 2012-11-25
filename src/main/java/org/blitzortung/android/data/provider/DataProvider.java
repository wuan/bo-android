package org.blitzortung.android.data.provider;

import java.util.List;

import android.content.pm.PackageInfo;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Participant;

public abstract class DataProvider {
	
	@SuppressWarnings("unused")
	private static final String TAG = "DataProvider";
	
	protected String username;
	
	protected String password;

    protected PackageInfo pInfo;

	public abstract void setUp();
	
	public abstract void shutDown();
	
	public abstract List<AbstractStroke> getStrokes(int intervalDuration, int intervalOffset, int region);
	
	public abstract List<AbstractStroke> getStrokesRaster(int intervalDuration, int intervalOffset, int params, int region);
	
	public abstract RasterParameters getRasterParameters();

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

    public abstract boolean isCapableOfHistoricalData();
}
