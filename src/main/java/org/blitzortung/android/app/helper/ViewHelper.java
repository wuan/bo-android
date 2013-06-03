package org.blitzortung.android.app.helper;

import android.util.DisplayMetrics;
import android.view.View;

public final class ViewHelper {
    
    private ViewHelper() {}

    public static float pxFromSp(View view, float sp) {
        final DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
        return sp * displayMetrics.scaledDensity;
    }

    public static float pxFromDp(View view, float dp)
    {
        final DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
        return dp * displayMetrics.density;
    }
}
