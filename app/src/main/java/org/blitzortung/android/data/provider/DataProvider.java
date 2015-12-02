package org.blitzortung.android.data.provider;

import android.content.pm.PackageInfo;

import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.provider.result.ResultEvent;

import java.util.List;

public abstract class DataProvider {

    protected String username;

    protected String password;

    protected PackageInfo pInfo;

    public abstract void setUp();

    public abstract void shutDown();

    public abstract void getStrikes(Parameters parameters, ResultEvent.ResultEventBuilder result);

    public abstract void getStrikesGrid(Parameters parameters, ResultEvent.ResultEventBuilder result);

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
