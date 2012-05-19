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
	
	public int getColorSection(long now, long eventTime, int minutesPerColor) {
		int section = (int) (now - eventTime) / 1000 / 60 / minutesPerColor;

		if (section >= getColors().length)
			section = getColors().length - 1;
		
		return section;
	}
	
	public int getColor(long now, long eventTime, int minutesPerColor) {
		return getColor(getColorSection(now, eventTime, minutesPerColor));
	}
	
	public int getColor(int section) {
		return getColors()[section];
	}
}