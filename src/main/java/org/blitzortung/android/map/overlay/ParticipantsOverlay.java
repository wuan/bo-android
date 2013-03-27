package org.blitzortung.android.map.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import org.blitzortung.android.app.R;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Participant.State;
import org.blitzortung.android.map.components.LayerOverlayComponent;
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class ParticipantsOverlay extends PopupOverlay<ParticipantOverlayItem> implements LayerOverlay {

    // VisibleForTesting
    protected final ArrayList<ParticipantOverlayItem> items;

    private final ParticipantColorHandler colorHandler;

    static private final Drawable DefaultDrawable;

    static {
        Shape shape = new ParticipantShape(1, 0);
        DefaultDrawable = new ShapeDrawable(shape);
    }

    private final EnumMap<State, Drawable> shapes = new EnumMap<State, Drawable>(State.class);

    private final LayerOverlayComponent layerOverlayComponent;

    // VisibleForTesting
    protected int shapeSize;

    public ParticipantsOverlay(Context context, ParticipantColorHandler colorHandler) {
        super(boundCenter(DefaultDrawable));

        layerOverlayComponent = new LayerOverlayComponent(context.getResources().getString(R.string.participants_layer));

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
            String label = item.getTitle();
            long lastDataTime = item.getLastDataTime();
            if (lastDataTime != 0) {
                label += "\n" + buildTimeString(lastDataTime);
            }
            showPopup(item.getPoint(), label);
            return true;
        }

        return false;
    }

    private String buildTimeString(long lastDataTime) {
        long now = System.currentTimeMillis();
        float time = (now - lastDataTime) / 1000.0f / 60.0f;

        if (time < 120) {
            return String.format("%.0f min", time);
        }

        time /= 60.0f;

        if (time < 48) {
            return String.format("%.1f h", time);
        }

        time /= 24.0f;

        return String.format("%.1f d", time);
    }

    @Override
    public String getName() {
        return layerOverlayComponent.getName();
    }

    @Override
    public boolean isEnabled() {
        return layerOverlayComponent.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        layerOverlayComponent.setEnabled(enabled);
    }

    @Override
    public boolean isVisible() {
        return layerOverlayComponent.isVisible();
    }

    @Override
    public void setVisibility(boolean visible) {
        layerOverlayComponent.setVisibility(visible);
    }
}