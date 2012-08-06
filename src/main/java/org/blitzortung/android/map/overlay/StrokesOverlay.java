package org.blitzortung.android.map.overlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
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

	private final ArrayList<StrokeOverlayItem> items;
	
	private final StrokeColorHandler colorHandler;
	
	private int zoomLevel;
	
	Raster raster = null;

	static private final Drawable DefaultDrawable;
	static {
		Shape shape = new StrokeShape(1, 0);
		DefaultDrawable = new ShapeDrawable(shape);
	}

	public StrokesOverlay(StrokeColorHandler colorHandler) {
		super(boundCenter(DefaultDrawable));

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
			
			if (isRaster()) {
				Paint paint = new Paint();
				paint.setColor(0xffffffff);
				paint.setStyle(Style.STROKE);
				Projection projection = mapView.getProjection();
				canvas.drawRect(raster.getRect(projection), paint);
			}
		}
	}

	public void addAndExpireStrokes(List<AbstractStroke> strokes, long expireTime) {
		if (isRaster()) {
			items.clear();
		}

		for (AbstractStroke stroke : strokes) {
			items.add(new StrokeOverlayItem(stroke));
		}

		expireStrokes(expireTime);

		setLastFocusedIndex(-1);
		populate();
	}

	private void expireStrokes(long expireTime) {
		List<StrokeOverlayItem> toRemove = new ArrayList<StrokeOverlayItem>();

		for (StrokeOverlayItem item : items) {
			if (item.getTimestamp() < expireTime) {
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
		
		colorHandler.updateTarget();

		Drawable drawable = null;

		for (StrokeOverlayItem item : items) {
			int section = colorHandler.getColorSection(now, item.getTimestamp(), getMinutesPerColor());

			if (isRaster() || current_section != section) {
				drawable = getDrawable(item, section, colorHandler);
			}

			item.setMarker(drawable);
		}
	}

	private Drawable getDrawable(StrokeOverlayItem item, int section, ColorHandler colorHandler) {

		Shape shape;

		Projection projection = this.getActivity().getMapView().getProjection();
		
		int color = colorHandler.getColor(section);

		if (isRaster()) {
			float lon_delta = raster.getLongitudeDelta() / 2.0f * 1e6f;
			float lat_delta = raster.getLatitudeDelta() / 2.0f * 1e6f;
			GeoPoint geoPoint = item.getPoint();
			Point center = projection.toPixels(geoPoint, null);
			Point topRight = projection.toPixels(new GeoPoint((int) (geoPoint.getLatitudeE6() + lat_delta),
					(int) (geoPoint.getLongitudeE6() + lon_delta)), null);
			Point bottomLeft = projection.toPixels(new GeoPoint((int) (geoPoint.getLatitudeE6() - lat_delta),
					(int) (geoPoint.getLongitudeE6() - lon_delta)), null);
			shape = new RasterShape(center, topRight, bottomLeft, color, item.getMultiplicity(), colorHandler.getTextColor());
		} else {
			shape = new StrokeShape(zoomLevel + 1, color);

		}
		return new ShapeDrawable(shape);
	}

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
			if (item != null && item.getPoint() != null && item.getTimestamp() != 0) {
				String result = (String) DateFormat.format("kk:mm:ss", new Date(item.getTimestamp()));

				if (item.getMultiplicity() > 1) {
					result += String.format(", #%d", item.getMultiplicity());
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

        return !eventHandled && clearPopup();
    }

	public int getTotalNumberOfStrokes() {

		int totalNumberOfStrokes = 0;

		for (StrokeOverlayItem item : items) {
			totalNumberOfStrokes += item.getMultiplicity();
		}

		return totalNumberOfStrokes;
	}
}