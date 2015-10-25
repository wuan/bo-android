package org.blitzortung.android.data.provider;

import android.content.pm.PackageInfo;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.RasterParameters;

import java.util.List;

public abstract class DataProvider {
	
	protected String username;
	
	protected String password;

    protected PackageInfo pInfo;

	public abstract void setUp();
	
	public abstract void shutDown();
	
	public abstract List<StrikeAbstract> getStrikes(int intervalDuration, int intervalOffset, int region);
    
    public abstract boolean returnsIncrementalData();
	
	public abstract List<StrikeAbstract> getStrikesGrid(int intervalDuration, int intervalOffset, int rasterSize, int countThreshold, int region);
	
	public abstract RasterParameters getRasterParameters();

    public abstract int[] getHistogram();
	
	public abstract List<Station> getStations(int region);
	
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
