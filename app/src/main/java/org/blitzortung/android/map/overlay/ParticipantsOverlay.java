package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.util.Log;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Station.State;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.components.LayerOverlayComponent;
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class ParticipantsOverlay extends PopupOverlay<ParticipantOverlayItem> implements LayerOverlay {

    static private final Drawable DefaultDrawable;

    static {
        DefaultDrawable = new ShapeDrawable(new ParticipantShape());
    }

    // VisibleForTesting
    protected final ArrayList<ParticipantOverlayItem> participants;
    private final ParticipantColorHandler colorHandler;
    private final EnumMap<State, ShapeDrawable> shapes;

    private final LayerOverlayComponent layerOverlayComponent;

    // VisibleForTesting
    private int zoomLevel;

    public ParticipantsOverlay(OwnMapActivity mapActivity, ParticipantColorHandler colorHandler) {
        super(mapActivity, boundCenter(DefaultDrawable));

        shapes = new EnumMap<>(State.class);
        shapes.put(State.ON, new ShapeDrawable(new ParticipantShape()));
        shapes.put(State.DELAYED, new ShapeDrawable(new ParticipantShape()));
        shapes.put(State.OFF, new ShapeDrawable(new ParticipantShape()));

        layerOverlayComponent = new LayerOverlayComponent(mapActivity.getResources().getString(R.string.participants_layer));
        this.colorHandler = colorHandler;

        participants = new ArrayList<>();
        populate();
    }

    @Override
    protected ParticipantOverlayItem createItem(int index) {
        return participants.get(index);
    }

    @Override
    public int size() {
        return participants.size();
    }

    @Override
    public void draw(Canvas canvas, com.google.android.maps.MapView mapView, boolean shadow) {
        if (!shadow) {
            super.draw(canvas, mapView, false);
        }
    }

    public void setParticipants(List<Station> stations) {
        Log.v(Main.LOG_TAG, String.format("ParticipantsOverlay.setStations() #%d", stations.size()));
        updateShapes();

        participants.clear();
        for (Station station : stations) {
            final ParticipantOverlayItem item = new ParticipantOverlayItem(station);
            item.setMarker(shapes.get(item.getParticipantState()));
            participants.add(item);
        }
        Log.v(Main.LOG_TAG, "ParticipantsOverlay.setStations() set");
        setLastFocusedIndex(-1);

        populate();
        Log.v(Main.LOG_TAG, "ParticipantsOverlay.setStations() finished");
    }

    public void clear() {
        setLastFocusedIndex(-1);
        clearPopup();
        participants.clear();
        populate();
    }

    public void updateZoomLevel(int zoomLevel) {
        if (zoomLevel != this.zoomLevel) {
            this.zoomLevel = zoomLevel;
            refresh();
        }
    }

    public void refresh() {
        updateShapes();

        for (ParticipantOverlayItem item : participants) {
            item.setMarker(shapes.get(item.getParticipantState()));
        }
    }

    private void updateShapes() {
        float shapeSize = (float) Math.max(1, zoomLevel - 3);
        colorHandler.updateTarget();

        int[] colors = colorHandler.getColors();
        updateShape(State.ON, shapeSize, colors[0]);
        updateShape(State.DELAYED, shapeSize, colors[1]);
        updateShape(State.OFF, shapeSize, colors[2]);
    }

    private void updateShape(State state, float shapeSize, int color) {
        ((ParticipantShape) shapes.get(state).getShape()).update(shapeSize, color);
    }

    @Override
    protected boolean onTap(int index) {
        ParticipantOverlayItem item = participants.get(index);

        if (item.getTitle() != null) {
            String label = item.getTitle();
            if (item.getParticipantState() != State.ON) {
                long lastDataTime = item.getLastDataTime();
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