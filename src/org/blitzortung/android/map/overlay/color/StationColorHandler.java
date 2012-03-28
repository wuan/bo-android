package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;

public class StationColorHandler extends ColorHandler {
	
	private int[] colorsOnSatelliteView = {0xff88ff22, 0xffff9900, 0xffff0000};
	
	private int[] mapColors = {0xff448811, 0xff884400, 0xff880000};

	public StationColorHandler(SharedPreferences preferences) {
		super(preferences);
	}
	
	@Override
	public int[] getColorsOnSatelliteView() {
		return colorsOnSatelliteView;
	}

	@Override
	public int[] getColorsOnMap() {
		return mapColors;
	}
}
