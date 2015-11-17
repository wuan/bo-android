package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;

public class StrikeShape extends Shape {

    private float size;
    private int color;

    public StrikeShape() {
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size / 4);
        canvas.drawLine(-size / 2, 0.0f, size / 2, 0.0f, paint);
        canvas.drawLine(0.0f, -size / 2, 0.0f, size / 2, paint);
    }

    public void update(float size, int color) {
        this.size = size;
        this.color = color;
    }
}
