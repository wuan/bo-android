package org.blitzortung.android.dialogs;

import android.content.SharedPreferences;
import org.blitzortung.android.map.overlay.color.ColorTarget;
import org.blitzortung.android.map.overlay.color.StrikeColorHandler;

public class AlertDialogColorHandler extends StrikeColorHandler {

    public AlertDialogColorHandler(SharedPreferences preferences) {
		super(preferences);
	}

	public int getTextColor(ColorTarget target) {
        switch (target) {
            default:
            case SATELLITE:
                return 0xff000000;
            case STREETMAP:
                return 0xffffffff;
        }
    }

    public int getLineColor(ColorTarget target) {
        switch (target) {
            default:
            case SATELLITE:
                return 0xff555555;
            case STREETMAP:
                return 0xff888888;
        }
    }

    public int getBackgroundColor(ColorTarget target) {
        switch (target) {
            default:
            case SATELLITE:
                return 0xff888888;
            case STREETMAP:
                return 0xff555555;
        }
    }
}