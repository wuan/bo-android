package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;

public class RasterShape extends Shape {

	private float x1;
	private float x2;
	private float y1;
	private float y2;
	private int color;
	private int alpha;
	private int multiplicity;

	public RasterShape(Point center, Point topRight, Point bottomLeft, int color, int multiplicity) {
		x1 = Math.max(Math.abs(center.x - bottomLeft.x), 1.5f);
		x2 = Math.max(Math.abs(center.x - topRight.x), 1.5f);
		y1 = Math.max(Math.abs(center.y - bottomLeft.y), 1.5f);
		y2 = Math.max(Math.abs(center.y - topRight.y), 1.5f);
		this.multiplicity = multiplicity;
		this.color = color;
		setAlphaValue();
		// Log.v("RasterShape",
		// String.format("x1: %.1f, x2: %.1f, y1: %.2f, y2: %.2f, alpha: %d",
		// x1, x2, y1, y2, alpha));
	}

	@Override
	public void draw(Canvas canvas, Paint paint) {
		resize(Math.max(x1, x2) * 2, Math.max(y1, y2) * 2);
		paint.setColor(color);
		paint.setAlpha(alpha);
		RectF r = new RectF(-x1, y2, x2, -y1);

		canvas.drawRect(r, paint);
		if (multiplicity > 0) {
			paint.setColor(0xffffffff);
			paint.setAlpha(alpha + 50);
			paint.setTextAlign(Align.CENTER);
			float textSize = (y1 + y2)/2.5f;
			paint.setTextSize(textSize);
			canvas.drawText(String.valueOf(multiplicity), 0.0f, 0.25f*textSize, paint);
		}
	}

	private void setAlphaValue() {
		float value = (x1 + x2 - 10) / 30;
		if (value < 0.0) {
			value = 0.0f;
		} else if (value > 1.0) {
			value = 1.0f;
		}

		alpha = 100 + (int) (155 * (1.0 - value));
	}

	@Override
	public boolean hasAlpha() {
		return alpha != 255;
	}

}
