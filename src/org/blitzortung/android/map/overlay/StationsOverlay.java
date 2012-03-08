package org.blitzortung.android.map.overlay;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Station.State;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.util.Log;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;

public class StationsOverlay extends ItemizedOverlay<StationOverlayItem> {
	
	private static final String TAG = "overlay.StationsOverlay";

	ArrayList<StationOverlayItem> items;

	static private Drawable DefaultDrawable;
	static {
		Shape shape = new StationShape(1, 0);
		DefaultDrawable = new ShapeDrawable(shape);
	}
	
	EnumMap<State, Drawable> shapes = new EnumMap<State, Drawable>(State.class);
	
	public StationsOverlay() {
		super(boundCenter(DefaultDrawable));

		items = new ArrayList<StationOverlayItem>();
		
		populate();
	}

	@Override
	protected StationOverlayItem createItem(int index) {
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

	public void setStations(List<Station> stations) {
		items.clear();
		for (Station station : stations) {
			items.add(new StationOverlayItem(station));
		}
		populate();
	}

	public void clear() {
		items.clear();
	}

	int shapeSize;

	int[] colors = { 0xffff0000, 0xffff9900, 0xffffff00, 0xff88ff22, 0xff00ffff, 0xff0000ff };

	public void updateShapeSize(int zoomLevel) {
		this.shapeSize = Math.max(1, zoomLevel - 3);
		
		refresh();
	}

	public void refresh() {
		shapes.clear();
		shapes.put(State.ON, getDrawable(0xff88ff22));
		shapes.put(State.DELAYED, getDrawable(0xffff9900));
		shapes.put(State.OFF, getDrawable(0xffff0000));

		for (StationOverlayItem item : items) {
			item.setMarker(shapes.get(item.getState()));
		}
	}

	private Drawable getDrawable(int color) {
		Shape shape = new StationShape(shapeSize, color);
		return new ShapeDrawable(shape);
	}

	@Override
	protected boolean onTap(int index) {
		Log.v(TAG, String.format("onTap(%d)", index));

		return false;
	}

}