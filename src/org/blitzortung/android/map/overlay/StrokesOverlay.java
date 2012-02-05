package org.blitzortung.android.map.overlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.data.beans.Stroke;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

public class StrokesOverlay extends ItemizedOverlay<StrokeOverlayItem> {

	private static final String TAG = "overlay.StrokesOverlay";

	ArrayList<StrokeOverlayItem> items;

	static private Drawable DefaultDrawable;
	static {
		Shape shape = new StrokeShape(1, 0);
		DefaultDrawable = new ShapeDrawable(shape);
	}

	public StrokesOverlay() {
		super(boundCenter(DefaultDrawable));

		items = new ArrayList<StrokeOverlayItem>();
		
		populate();
	}

	@Override
	protected StrokeOverlayItem createItem(int index) {
		return items.get(index);
	}

	@Override
	public int size() {
		return items.size();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (!shadow) {
			super.draw(canvas, mapView, false);
		}
	}

	public void addStrokes(List<Stroke> strokes) {
		for (Stroke stroke : strokes) {
			items.add(new StrokeOverlayItem(stroke));
		}
		populate();
	}

	public void clear() {
		items.clear();
	}

	int shapeSize;

	int[] colors = { 0xffff0000, 0xffff9900, 0xffffff00, 0xff88ff22, 0xff00ffff, 0xff0000ff };

	public void updateShapeSize(int shapeSize) {
		this.shapeSize = shapeSize;
		
		refresh();
	}

	public void refresh() {
		Date now = new GregorianCalendar().getTime();

		int current_section = -1;

		Drawable drawable = null;

		for (StrokeOverlayItem item : items) {
			int section = (int) (now.getTime() - item.getTimestamp().getTime()) / 1000 / 60 / 10;

			if (section >= colors.length)
				section = colors.length - 1;

			if (current_section != section) {
				drawable = getDrawable(section);
			}

			item.setMarker(drawable);
		}
	}

	private Drawable getDrawable(int section) {
		Shape shape = new StrokeShape(shapeSize, colors[section]);
		return new ShapeDrawable(shape);
	}

	@Override
	protected boolean onTap(int index) {
		Log.v(TAG, String.format("onTap(%d)", index));

		return false;
	}

	public void expireStrokes(Date expireTime) {
		List<StrokeOverlayItem> toRemove = new ArrayList<StrokeOverlayItem>();
		
		for (StrokeOverlayItem item : items) {
			if (item.getTimestamp().before(expireTime)) {
				toRemove.add(item);
			} else {
				break;
			}
		}
		
		if (toRemove.size() > 0) {
			items.removeAll(toRemove);
		}
	}
}