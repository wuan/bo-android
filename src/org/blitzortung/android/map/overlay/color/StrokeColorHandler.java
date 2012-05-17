package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;

public class StrokeColorHandler extends ColorHandler {
	
	private int[] colorsOnSatelliteView = { 0xffff0000, 0xffff9900, 0xffffff00, 0xff99ff22, 0xff00ffff, 0xff6699ff };
	
	private int[] mapColors = { 0xffcc2200, 0xffaa5500, 0xffaaaa00, 0xff44aa11, 0xff00aaaa, 0xff2222cc };

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
