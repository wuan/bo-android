package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.Shape;

public class StationShape extends Shape {

	private float width;
	private float height;
	private int color;
	
	public StationShape(float size, int color) {
		width = size;
		height = size;
		this.color = color;
	}
	
	@Override
	public void draw(Canvas canvas, Paint paint) {
		paint.setColor(color);
		canvas.drawRect(-width / 2, height / 2, width / 2, -height / 2, paint);
	}
}
