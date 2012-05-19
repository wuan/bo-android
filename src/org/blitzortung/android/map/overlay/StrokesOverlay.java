package org.blitzortung.android.map.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.format.DateFormat;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

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

		setLastFocusedIndex(-1);
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
		clearPopup();
		items.clear();
		setLastFocusedIndex(-1);
		populate();
	}

	int zoomLevel;

	public void updateZoomLevel(int zoomLevel) {
		this.zoomLevel = zoomLevel;

		refresh();
	}

	public int getMinutesPerColor() {
		return 10;
	}
	
	public ColorHandler getColorHandler() {
		return colorHandler;
	}
	
	public void refresh() {
		long now = new GregorianCalendar().getTime().getTime();

		int current_section = -1;

		Drawable drawable = null;

		for (StrokeOverlayItem item : items) {
			int section = colorHandler.getColorSection(now, item.getTimestamp().getTime(), getMinutesPerColor());

			if (isRaster() || current_section != section) {
				drawable = getDrawable(item.getPoint(), section, colorHandler.getColor(section));
			}

			item.setMarker(drawable);
		}
	}

	private Drawable getDrawable(GeoPoint point, int section, int color) {

		Shape shape = null;

		Projection projection = this.getActivity().getMapView().getProjection();

		if (isRaster()) {
			float lon_delta = raster.getLongitudeDelta() / 2.0f * 1e6f;
			float lat_delta = raster.getLatitudeDelta() / 2.0f * 1e6f;
			Point center = projection.toPixels(point, null);
			Point topRight = projection.toPixels(new GeoPoint((int) (point.getLatitudeE6() + lat_delta),
					(int) (point.getLongitudeE6() + lon_delta)), null);
			Point bottomLeft = projection.toPixels(new GeoPoint((int) (point.getLatitudeE6() - lat_delta),
					(int) (point.getLongitudeE6() - lon_delta)), null);
			// Log.v("StrokesOverlay", "" + center + " " + topRight + " " +
			// bottomLeft + " raster: " + raster + " point: " + point);
			shape = new RasterShape(center, topRight, bottomLeft, color);
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

	@Override
	public boolean onTap(GeoPoint arg0, MapView arg1) {
		boolean eventHandled = super.onTap(arg0, arg1);

		if (!eventHandled) {
			return clearPopup();
		}
		
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