package org.blitzortung.android.data;

import org.blitzortung.android.data.provider.DataResult;

public interface DataListener {

    public void onBeforeDataUpdate();

	public void onDataUpdate(DataResult result);
	
	public void onDataReset();

    public void onDataError();
}
