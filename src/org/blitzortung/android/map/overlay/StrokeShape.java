package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;

public class StrokeShape extends Shape {

	private final float size;
	private final int color;
	
	public StrokeShape(float size, int color) {
		this.size = size;
		this.color = color;
	}
	
	@Override
	public void draw(Canvas canvas, Paint paint) {
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(size/4);
		canvas.drawLine(-size / 2, 0.0f, size / 2, 0.0f, paint);
		canvas.drawLine(0.0f, -size / 2, 0.0f, size / 2, paint);
	}
}
