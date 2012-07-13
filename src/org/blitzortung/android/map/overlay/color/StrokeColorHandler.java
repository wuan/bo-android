package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;

public class StrokeColorHandler extends ColorHandler {
	
	private int[] satelliteViewColors = { 0xffff0000, 0xffff9900, 0xffffff00, 0xff99ff22, 0xff00ffff, 0xff6699ff };
	
	private int[] mapColors = { 0xffcc2200, 0xffaa5500, 0xffaaaa00, 0xff44aa11, 0xff00aaaa, 0xff2222cc };

	public StrokeColorHandler(SharedPreferences preferences) {
		super(preferences);
	}
	
	@Override
	public int[] getColors(ColorTarget target) {
		switch(target) {
		case SATELLITE:
			return satelliteViewColors;

		case STREETMAP:
			return mapColors;			
		}
		throw new IllegalStateException("Unhandled color target " + target);
	}
	
	@Override
	public int getTextColor(ColorTarget target) {
		switch(target) {
		case SATELLITE:
			return 0xff000000;

		case STREETMAP:
			return 0xffffffff;			
		}
		throw new IllegalStateException("Unhandled color target " + target);
	}
	
}
