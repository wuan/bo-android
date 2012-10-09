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

public class ParticipantsOverlay extends PopupOverlay<ParticipantOverlayItem> {

    // VisibleForTesting
    protected final ArrayList<ParticipantOverlayItem> items;

    private final ParticipantColorHandler colorHandler;

    static private final Drawable DefaultDrawable;

    static {
        Shape shape = new ParticipantShape(1, 0);
        DefaultDrawable = new ShapeDrawable(shape);
    }

    private final EnumMap<State, Drawable> shapes = new EnumMap<State, Drawable>(State.class);

    // VisibleForTesting
    protected int shapeSize;

    public ParticipantsOverlay(ParticipantColorHandler colorHandler) {
        super(boundCenter(DefaultDrawable));

        this.colorHandler = colorHandler;

        items = new ArrayList<ParticipantOverlayItem>();

        setLastFocusedIndex(-1);
        populate();
    }

    @Override
    protected ParticipantOverlayItem createItem(int index) {
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
            items.add(new ParticipantOverlayItem(station));
        }
        populate();
    }

    public void clear() {
        items.clear();
        setLastFocusedIndex(-1);
        populate();
    }

    public void updateZoomLevel(int zoomLevel) {
        shapeSize = Math.max(1, zoomLevel - 3);

        refresh();
    }

    public void refresh() {

        int[] colors = colorHandler.getColors();

        shapes.clear();
        shapes.put(State.ON, getDrawable(colors[0]));
        shapes.put(State.DELAYED, getDrawable(colors[1]));
        shapes.put(State.OFF, getDrawable(colors[2]));

        for (ParticipantOverlayItem item : items) {
            item.setMarker(shapes.get(item.getState()));
        }
    }

    // VisibleForTesting
    protected Drawable getDrawable(int color) {
        Shape shape = new ParticipantShape(shapeSize, color);
        return new ShapeDrawable(shape);
    }

    @Override
    protected boolean onTap(int index) {
        ParticipantOverlayItem item = items.get(index);

        if (item.getTitle() != null) {
            showPopup(item.getPoint(), item.getTitle());
            return true;
        }

        return false;
    }

}