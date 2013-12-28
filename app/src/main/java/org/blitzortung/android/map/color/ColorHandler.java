package org.blitzortung.android.map.color;

import android.content.SharedPreferences;
import android.graphics.Color;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.TimeIntervalWithOffset;

public abstract class ColorHandler {

    private final SharedPreferences preferences;

    private ColorScheme colorScheme;

    private ColorTarget target;

    public ColorHandler(SharedPreferences preferences) {
        this.preferences = preferences;
        updateTarget();
    }

    public void updateTarget() {
        target = ColorTarget.valueOf(preferences.getString(PreferenceKey.MAP_TYPE.toString(), "SATELLITE"));
        colorScheme = ColorScheme.valueOf(preferences.getString(PreferenceKey.COLOR_SCHEME.toString(), ColorScheme.BLITZORTUNG.toString()));
    }

    public final int[] getColors() {
        return getColors(target);
    }

    abstract protected int[] getColors(ColorTarget target);

    public int getColorSection(long now, long eventTime, TimeIntervalWithOffset timeIntervalWithOffset) {
        return getColorSection(now, eventTime, timeIntervalWithOffset.getIntervalDuration(), timeIntervalWithOffset.getIntervalOffset());
    }

    private int getColorSection(long now, long eventTime, int intervalDuration, int intervalOffset) {
        int minutesPerColor = intervalDuration / getColors().length;
        int section = (int) (now + intervalOffset * 60 * 1000 - eventTime) / 1000 / 60 / minutesPerColor;
        section = limitIndexToValidRange(section);

        return section;
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    public int getColor(long now, long eventTime, int intervalDuration) {
        return getColor(getColorSection(now, eventTime, intervalDuration, 0));
    }

    public int getColor(int section) {
        section = limitIndexToValidRange(section);
        return getColors()[section];
    }

    private int limitIndexToValidRange(int section) {
        section = Math.min(section, getColors().length - 1);
        section = Math.max(section, 0);
        return section;
    }

    public final int getTextColor() {
        return getTextColor(target);
    }

    public int getTextColor(ColorTarget target) {
        switch (target) {
            default:
            case SATELLITE:
                return 0xffffffff;
            case STREETMAP:
                return 0xff000000;
        }
    }

    public final int getLineColor() {
        return getLineColor(target);
    }

    public int getLineColor(ColorTarget target) {
        switch (target) {
            default:
            case SATELLITE:
                return 0xffffffff;
            case STREETMAP:
                return 0xff000000;
        }
    }

    public int getBackgroundColor() {
        return getBackgroundColor(target);
    }

    public int getBackgroundColor(ColorTarget target) {
        switch (target) {
            default:
            case SATELLITE:
                return 0x00000000;
            case STREETMAP:
                return 0x00ffffff;
        }
    }

    public int getNumberOfColors() {
        return getColors().length;
    }

    public int[] modifyBrightness(int[] colors, float factor) {
        int[] result = new int[colors.length];

        float[] HSVValues = new float[3];

        for (int index = 0; index < colors.length; index++) {
            Color.colorToHSV(colors[index], HSVValues);
            HSVValues[2] *= factor;
            result[index] = Color.HSVToColor(HSVValues);
        }

        return result;
    }

}