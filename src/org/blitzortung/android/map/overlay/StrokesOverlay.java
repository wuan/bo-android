package org.blitzortung.android.map.overlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.format.DateFormat;

public class StrokesOverlay extends PopupOverlay<StrokeOverlayItem> {

	@SuppressWarnings("unused")
	private static final String TAG = "overlay.StrokesOverlay";

	ArrayList<StrokeOverlayItem> items;
	StrokeColorHandler colorHandler;

	static private Drawable DefaultDrawable;
	static {
		Shape shape = new StrokeShape(1, 0);
		DefaultDrawable = new ShapeDrawable(shape);
	}

	public StrokesOverlay(OwnMapActivity activity, StrokeColorHandler colorHandler) {
		super(activity, boundCenter(DefaultDrawable));

		this.colorHandler = colorHandler;

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
	public void draw(Canvas canvas, com.google.android.maps.MapView mapView, boolean shadow) {
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

	public void updateShapeSize(int zoomLevel) {
		this.shapeSize = zoomLevel + 1;

		refresh();
	}

	public void refresh() {
		Date now = new GregorianCalendar().getTime();

		int current_section = -1;

		Drawable drawable = null;

		int colors[] = colorHandler.getColors();

		for (StrokeOverlayItem item : items) {
			int section = (int) (now.getTime() - item.getTimestamp().getTime()) / 1000 / 60 / 10;

			if (section >= colors.length)
				section = colors.length - 1;

			if (current_section != section) {
				drawable = getDrawable(section, colors[section]);
			}

			item.setMarker(drawable);
		}
	}

	private Drawable getDrawable(int section, int color) {

		Shape shape = new StrokeShape(shapeSize, color);
		return new ShapeDrawable(shape);
	}

	@Override
	protected boolean onTap(int index) {

		if (index < items.size()) {
			StrokeOverlayItem item = items.get(index);
			if (item != null && item.getPoint() != null && item.getTimestamp() != null) {
				showPopup(item.getPoint(), (String) DateFormat.format("kk:mm:ss", item.getTimestamp()));
				return true;

			}
		}
		
		clearPopup();

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