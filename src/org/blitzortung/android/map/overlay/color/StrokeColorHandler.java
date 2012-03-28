package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;

public class StrokeColorHandler extends ColorHandler {
	
	private int[] colorsOnSatelliteView = { 0xffff0000, 0xffff9900, 0xffffff00, 0xff99ff22, 0xff00ffff, 0xff99bbff };
	
	private int[] mapColors = { 0xff880000, 0xff884400, 0xff888800, 0xff448811, 0xff008888, 0xff222288 };

	public StrokeColorHandler(SharedPreferences preferences) {
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
