package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;

public abstract class ColorHandler {

	SharedPreferences preferences;
	
	public ColorHandler(SharedPreferences preferences) {
		this.preferences = preferences;
	}
	
	public int[] getColors() {
		if (preferences.getString("map_mode", "SATELLITE").equals("SATELLITE"))
			return getColorsOnSatelliteView();
		else 
			return getColorsOnMap();
	}
	
	abstract int[] getColorsOnSatelliteView();
	
	abstract int[] getColorsOnMap();
}