package org.blitzortung.android.data.provider;

import java.util.List;

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;

public interface DataProvider {
	public List<Stroke> getStrokes();
	
	public List<Station> getStations();
}
