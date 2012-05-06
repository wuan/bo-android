package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;

public class RasterShape extends Shape {

	private float width;
	private float height;
	//private float lineWidth;
	private int color;
	private int alpha;
	
	public RasterShape(float width, float height, int color) {
		this.width = width;
		this.height = height;
		//lineWidth = size/4;
		this.color = color;
		setAlphaValue();
	}
	
	@Override
	public void draw(Canvas canvas, Paint paint) {
		resize(width, height);
		paint.setColor(color);
		paint.setAlpha(alpha);
		RectF r = new RectF(-width / 2, height / 2, width / 2, -height / 2);
		
		canvas.drawRect(r, paint);
	}
	
	private void setAlphaValue() {
		float value = width / 30;
		if (value < 0.0) {
			value = 0.0f;
		} else if (value > 1.0) {
			value = 1.0f;
		}
		
		alpha = 100 + (int)(155 * (1.0 - value));
	}
	
	@Override
	public boolean hasAlpha() {
		return alpha != 255;
	}
	
}
