package org.blitzortung.android.map.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
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

	public void addAndExpireStrokes(List<AbstractStroke> strokes, Date expireTime) {
		
		Collections.reverse(items);
		
		if (isRaster()) {
			items.clear();
		}
		
		for (AbstractStroke stroke : strokes) {
			items.add(new StrokeOverlayItem(stroke));
		}
		
		expireStrokes(expireTime);
		
		Collections.reverse(items);
		
		populate();
	}

	private void expireStrokes(Date expireTime) {
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
	
	public void clear() {
		items.clear();
		populate();
	}

	int zoomLevel;

	public void updateZoomLevel(int zoomLevel) {
		this.zoomLevel = zoomLevel;

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
		populate();
	}

	private Drawable getDrawable(int section, int color) {

		Shape shape = null;
		
		if (isRaster()) {
			float scaleFactor = (float)(Math.pow(2, zoomLevel - 1) * 1.1);
			shape = new RasterShape(scaleFactor * raster.getLongitudeDelta() * 0.63f , scaleFactor * raster.getLatitudeDelta(), color);
		} else {
			  shape = new StrokeShape(zoomLevel + 1, color);
			
		}
		return new ShapeDrawable(shape);
	}

	Raster raster = null;
	
	public void setRaster(Raster raster) {
		this.raster = raster;
	}
	
	public boolean isRaster() {
		return raster != null;
	}
	
	@Override
	protected boolean onTap(int index) {

		if (index < items.size()) {
			StrokeOverlayItem item = items.get(index);
			if (item != null && item.getPoint() != null && item.getTimestamp() != null) {
				String result = (String) DateFormat.format("kk:mm:ss", item.getTimestamp());
				
				if (item.getMultitude() > 1) {
					result += String.format(", #%d", item.getMultitude());
				}
				showPopup(item.getPoint(), result);
				return true;

			}
		}
		
		clearPopup();

		return false;
	}

	public int getTotalNumberOfStrokes() {
		
		int totalNumberOfStrokes = 0;
		
		for (StrokeOverlayItem item : items) {
			totalNumberOfStrokes += item.getMultitude();
		}
		
		return totalNumberOfStrokes;
	}
}