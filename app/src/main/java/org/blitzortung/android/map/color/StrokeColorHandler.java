package org.blitzortung.android.map.color;

import android.content.SharedPreferences;

public class StrokeColorHandler extends ColorHandler {

	public StrokeColorHandler(SharedPreferences preferences) {
		super(preferences);
	}

	@Override
	public int[] getColors(ColorTarget target) {
		switch(target) {
		case SATELLITE:
			return getColorScheme().getStrokeColors();

		case STREETMAP:
			return modifyBrightness(getColorScheme().getStrokeColors(), 0.8f);
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
