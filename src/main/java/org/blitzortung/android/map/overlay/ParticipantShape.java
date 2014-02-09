package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.shapes.Shape;

public class ParticipantShape extends Shape {

    private final RectF rect;
	private int color;
	
	public ParticipantShape() {
        rect = new RectF();
		color = 0x00000000;
	}
	
	@Override
	public void draw(Canvas canvas, Paint paint) {
		paint.setColor(color);
        paint.setAlpha(0xff);
        paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(rect, paint);
	}
    
    public void update(float size, int color) {
        float halfSize = size / 2f;
        rect.set(-halfSize, -halfSize, halfSize, halfSize);
        resize(rect.width(), rect.width());
        
        this.color = color;
    }
}
