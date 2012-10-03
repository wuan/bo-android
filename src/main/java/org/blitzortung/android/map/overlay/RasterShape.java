package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;

public class RasterShape extends Shape {

    private final RectF rect;
    private final int color;
    private int alpha;
    private final int multiplicity;
    private final int textColor;

    public RasterShape(Point center, Point topRight, Point bottomLeft, int color, int multiplicity, int textColor) {

        float x1 = Math.max(Math.abs(center.x - bottomLeft.x), 1.5f);
        float x2 = Math.max(Math.abs(center.x - topRight.x), 1.5f);
        float y1 = Math.max(Math.abs(center.y - bottomLeft.y), 1.5f);
        float y2 = Math.max(Math.abs(center.y - topRight.y), 1.5f);
        resize(Math.max(x1, x2) * 2, Math.max(y1, y2) * 2);

        rect = new RectF(-x1, y2, x2, -y1);
        this.multiplicity = multiplicity;
        this.color = color;
        this.textColor = textColor;

        setAlphaValue();
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        paint.setAlpha(alpha);
        canvas.drawRect(rect, paint);

        paint.setColor(textColor);
        paint.setAlpha(alpha + 50);
        paint.setTextAlign(Align.CENTER);
        float textSize = rect.height() / 2.5f;
        paint.setTextSize(textSize);
        canvas.drawText(String.valueOf(multiplicity), 0.0f, 0.4f * textSize, paint);
    }

    private void setAlphaValue() {
        float value = (rect.width() - 10) / 30;
        value = Math.min(Math.max(value, 0.0f), 1.0f);
        alpha = 100 + (int) (155 * (1.0 - value));
    }

    @Override
    public boolean hasAlpha() {
        return alpha != 255;
    }

}
