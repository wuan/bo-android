package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;

public abstract class ColorHandler {

	private final SharedPreferences preferences;
	
	private ColorTarget target;

	public ColorHandler(SharedPreferences preferences) {
		this.preferences = preferences;
		updateTarget(); 
	}

	public void updateTarget() {
		target = ColorTarget.valueOf(preferences.getString("map_mode", "SATELLITE"));
	}
	
	public final int[] getColors() {
		return getColors(target);
	}

	abstract protected int[] getColors(ColorTarget target);

	public int getColorSection(long now, long eventTime, int minutesPerColor) {
		int section = (int) (now - eventTime) / 1000 / 60 / minutesPerColor;

		section = Math.min(section, getColors().length - 1);
		section = Math.max(section, 0);

		return section;
	}

	public int getColor(long now, long eventTime, int minutesPerColor) {
		return getColor(getColorSection(now, eventTime, minutesPerColor));
	}

	public int getColor(int section) {
		return getColors()[section];
	}

	public final int getTextColor() {
		return getTextColor(target);
	}
	
	public int getTextColor(ColorTarget target) {
		return 0xffffffff;
	}

    public int getLineColor() {
        switch (target) {
            case SATELLITE:
                return 0xffffffff;
            case STREETMAP:
                return 0xff000000;
        }
        return 0;
    }

    public int getBackgroundColor() {
        switch (target) {
            case SATELLITE:
                return 0xff000000;
            case STREETMAP:
                return 0xffffffff;
        }
        return 0;
    }

    public int getNumberOfColors() {
        return getColors().length;
    }
}