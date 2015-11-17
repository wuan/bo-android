package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;

public class StrikeColorHandler extends ColorHandler {

    public StrikeColorHandler(SharedPreferences preferences) {
        super(preferences);
    }

    @Override
    public int[] getColors(ColorTarget target) {
        switch (target) {
            case SATELLITE:
                return getColorScheme().getStrikeColors();

            case STREETMAP:
                return modifyBrightness(getColorScheme().getStrikeColors(), 0.8f);
        }
        throw new IllegalStateException("Unhandled color target " + target);
    }

    @Override
    public int getTextColor(ColorTarget target) {
        switch (target) {
            case SATELLITE:
                return 0xff000000;

            case STREETMAP:
                return 0xffffffff;
        }
        throw new IllegalStateException("Unhandled color target " + target);
    }
}
