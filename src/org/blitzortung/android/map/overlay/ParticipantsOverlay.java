package org.blitzortung.android.map.overlay;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Participant.State;
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;

public class ParticipantsOverlay extends PopupOverlay<StationOverlayItem> {

	final ArrayList<StationOverlayItem> items;

	final ParticipantColorHandler colorHandler;

	static private final Drawable DefaultDrawable;
	static {
		Shape shape = new StationShape(1, 0);
		DefaultDrawable = new ShapeDrawable(shape);
	}

	final EnumMap<State, Drawable> shapes = new EnumMap<State, Drawable>(State.class);

	public ParticipantsOverlay(ParticipantColorHandler colorHandler) {
		super(boundCenter(DefaultDrawable));

		this.colorHandler = colorHandler;

		items = new ArrayList<StationOverlayItem>();

		setLastFocusedIndex(-1);
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
	public void draw(Canvas canvas, com.google.android.maps.MapView mapView, boolean shadow) {
		if (!shadow) {
			super.draw(canvas, mapView, false);
		}
	}

	public void setParticipants(List<Participant> stations) {
		items.clear();
		for (Participant station : stations) {
			items.add(new StationOverlayItem(station));
		}
		populate();
	}

	public void clear() {
		items.clear();
		setLastFocusedIndex(-1);
		populate();
	}

	int shapeSize;

	int[] colors = { 0xffff0000, 0xffff9900, 0xffffff00, 0xff88ff22, 0xff00ffff, 0xff0000ff };

	public void updateZoomLevel(int zoomLevel) {
		this.shapeSize = Math.max(1, zoomLevel - 3);

		refresh();
	}

	public void refresh() {

		int[] colors = colorHandler.getColors();

		shapes.clear();
		shapes.put(State.ON, getDrawable(colors[0]));
		shapes.put(State.DELAYED, getDrawable(colors[1]));
		shapes.put(State.OFF, getDrawable(colors[2]));

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

		if (index < items.size()) {
			StationOverlayItem item = items.get(index);

			if (item != null && item.getTitle() != null) {
				showPopup(item.getPoint(), item.getTitle());
				return true;
			}
		}
		
		clearPopup();
		return false;
	}

}