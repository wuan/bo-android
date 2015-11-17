package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;

public class RasterShape extends Shape {

    private static final float MIN_SIZE = 1.5f;

    private final RectF rect;
    private int color;
    private int alpha;
    private int multiplicity;
    private int textColor;

    public RasterShape() {
        rect = new RectF();
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        paint.setAlpha(alpha);
        canvas.drawRect(rect, paint);

        float textSize = rect.height() / 2.5f;
        if (textSize >= 8f) {
            paint.setColor(textColor);
            paint.setAlpha(0xff);
            paint.setTextAlign(Align.CENTER);
            paint.setTextSize(textSize);
            canvas.drawText(String.valueOf(multiplicity), 0.0f, 0.4f * textSize, paint);
        }
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

    public void update(Point topLeft, Point bottomRight, int color, int multiplicity, int textColor) {
        float x1 = Math.min(topLeft.x, -MIN_SIZE);
        float y1 = Math.min(topLeft.y, -MIN_SIZE);
        float x2 = Math.max(bottomRight.x, MIN_SIZE);
        float y2 = Math.max(bottomRight.y, MIN_SIZE);
        rect.set(x1, y1, x2, y2);
        resize(rect.width(), rect.height());

        this.multiplicity = multiplicity;
        this.color = color;
        this.textColor = textColor;

        setAlphaValue();
    }
}
