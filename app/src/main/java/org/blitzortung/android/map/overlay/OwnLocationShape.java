package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;

public class OwnLocationShape extends Shape {

    private final float size;

    public OwnLocationShape(float size) {
        this.size = size;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x88555555);
        canvas.drawCircle(0.0f, 0.0f, size / 1.3f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size / 4);
        paint.setColor(0xffffffff);
        canvas.drawLine(-size / 2, 0.0f, size / 2, 0.0f, paint);
        canvas.drawLine(0.0f, -size / 2, 0.0f, size / 2, paint);
        paint.setColor(0xff33aaff);
        canvas.drawCircle(0.0f, 0.0f, size / 1.3f, paint);
    }

}
