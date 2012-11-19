package org.blitzortung.android.map.overlay;

import android.graphics.*;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class DimOverlay extends Overlay {

    private final ColorHandler colorHandler;

    public DimOverlay(ColorHandler colorHandler) {
        this.colorHandler = colorHandler;
    }
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (!shadow) {
            Rect rect = canvas.getClipBounds();
            Paint paint = new Paint();
            paint.setColor(colorHandler.getBackgroundColor());
            paint.setAlpha(120);
            canvas.drawRect(rect, paint);
        }
    }



}
