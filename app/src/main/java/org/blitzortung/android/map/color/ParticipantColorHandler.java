package org.blitzortung.android.map.color;

import android.content.SharedPreferences;

public class ParticipantColorHandler extends ColorHandler {
	
	private final int[] satelliteViewColors = {0xff88ff22, 0xffff9900, 0xffff0000};
	
	private final int[] mapColors = {0xff448811, 0xff884400, 0xff880000};

	public ParticipantColorHandler(SharedPreferences preferences) {
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
}
