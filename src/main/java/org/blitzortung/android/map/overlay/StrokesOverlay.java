package org.blitzortung.android.map.overlay;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.format.DateFormat;
import android.util.Log;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.data.TimeIntervalWithOffset;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.map.components.LayerOverlayComponent;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StrokesOverlay extends PopupOverlay<StrokeOverlayItem> implements TimeIntervalWithOffset, LayerOverlay {

    // VisibleForTesting
    protected final ArrayList<StrokeOverlayItem> strokes;

    private final StrokeColorHandler colorHandler;

    private final LayerOverlayComponent layerOverlayComponent;

    private int zoomLevel;

    private RasterParameters rasterParameters = null;

    static private final Drawable DefaultDrawable;

    private int intervalDuration;

    private int intervalOffset;

    private int region;

    private long referenceTime;

    static {
        StrokeShape shape = new StrokeShape();
        shape.update(1, 0);
        DefaultDrawable = new ShapeDrawable(shape);
    }

    public StrokesOverlay(Context context, StrokeColorHandler colorHandler) {
        super(boundCenter(DefaultDrawable));

        layerOverlayComponent = new LayerOverlayComponent(context.getResources().getString(R.string.strokes_layer));
        this.colorHandler = colorHandler;

        strokes = new ArrayList<StrokeOverlayItem>();

        populate();
    }

    @Override
    protected StrokeOverlayItem createItem(int index) {
        return strokes.get(index);
    }

    @Override
    public int size() {
        return strokes.size();
    }

    @Override
    public void draw(Canvas canvas, com.google.android.maps.MapView mapView, boolean shadow) {
        if (!shadow) {
            super.draw(canvas, mapView, false);

            if (hasRasterParameters()) {
                drawDataAreaRect(canvas, mapView);
            }
        }
    }

    private void drawDataAreaRect(Canvas canvas, MapView mapView) {
        Paint paint = new Paint();
        paint.setColor(colorHandler.getLineColor());
        paint.setStyle(Style.STROKE);

        Rect clipBounds = canvas.getClipBounds();
        RectF rect = rasterParameters.getRect(mapView.getProjection());

        if (rect.left >= clipBounds.left && rect.left <= clipBounds.right) {
            canvas.drawLine(rect.left, Math.max(rect.top, clipBounds.top), rect.left, Math.min(rect.bottom, clipBounds.bottom), paint);
        }
        if (rect.right >= clipBounds.left && rect.right <= clipBounds.right) {
            canvas.drawLine(rect.right, Math.max(rect.top, clipBounds.top), rect.right, Math.min(rect.bottom, clipBounds.bottom), paint);
        }
        if (rect.bottom <= clipBounds.bottom && rect.bottom >= clipBounds.top) {
            canvas.drawLine(Math.max(rect.left, clipBounds.left), rect.bottom, Math.min(rect.right, clipBounds.right), rect.bottom, paint);
        }
        if (rect.top <= clipBounds.bottom && rect.top >= clipBounds.top) {
            canvas.drawLine(Math.max(rect.left, clipBounds.left), rect.top, Math.min(rect.right, clipBounds.right), rect.top, paint);
        }
    }

    public void addStrokes(List<AbstractStroke> strokes) {
        Log.v(Main.LOG_TAG, "StrokesOverlay.addStrokes() #" + strokes.size());
        for (AbstractStroke stroke : strokes) {
            this.strokes.add(new StrokeOverlayItem(stroke));
        }
        Log.v(Main.LOG_TAG, "StrokesOverlay.addStrokes() added");
        setLastFocusedIndex(-1);
        populate();
        Log.v(Main.LOG_TAG, "StrokesOverlay.addStrokes() finished");
    }

    public void expireStrokes() {
        long expireTime = referenceTime - (intervalDuration - intervalOffset) * 60 * 1000;
        List<StrokeOverlayItem> toRemove = new ArrayList<StrokeOverlayItem>();

        for (StrokeOverlayItem item : strokes) {
            if (item.getTimestamp() < expireTime) {
                toRemove.add(item);
            } else {
                break;
            }
        }

        if (toRemove.size() > 0) {
            strokes.removeAll(toRemove);
        }
    }

    public void clear() {
        setLastFocusedIndex(-1);
        clearPopup();
        strokes.clear();
        populate();
    }

    public void updateZoomLevel(int zoomLevel) {
        if (hasRasterParameters() || zoomLevel != this.zoomLevel) {
            this.zoomLevel = zoomLevel;
            refresh();
        }
    }

    public ColorHandler getColorHandler() {
        return colorHandler;
    }

    public void refresh() {
        long now = System.currentTimeMillis();

        int current_section = -1;

        colorHandler.updateTarget();

        Shape drawable = null;

        for (StrokeOverlayItem item : strokes) {
            int section = colorHandler.getColorSection(now, item.getTimestamp(), this);

            if (hasRasterParameters() || current_section != section) {
                drawable = updateAndReturnDrawable(item, section, colorHandler);
            } else {
                item.setShape(drawable);
            }
        }
    }

    // VisibleForTesting
    protected Shape updateAndReturnDrawable(StrokeOverlayItem item, int section, ColorHandler colorHandler) {
        final Projection projection = getActivity().getMapView().getProjection();
        final int color = colorHandler.getColor(section);
        final int textColor = colorHandler.getTextColor();

        item.updateShape(getRasterParameters(), projection, color, textColor, zoomLevel);

        return item.getShape();
    }

    public void setRasterParameters(RasterParameters rasterParameters) {
        this.rasterParameters = rasterParameters;
    }

    public boolean hasRasterParameters() {
        return rasterParameters != null;
    }

    public RasterParameters getRasterParameters() {
        return rasterParameters;
    }

    public boolean hasRealtimeData() {
        return intervalOffset == 0;
    }

    @Override
    protected boolean onTap(int index) {
        StrokeOverlayItem item = strokes.get(index);
        if (item.getPoint() != null && item.getTimestamp() != 0) {
            String result = (String) DateFormat.format("kk:mm:ss", item.getTimestamp());

            if (item.getMultiplicity() > 1) {
                result += String.format(", #%d", item.getMultiplicity());
            }
            showPopup(item.getPoint(), result);
            return true;
        }
        return false;
    }

    public int getTotalNumberOfStrokes() {

        int totalNumberOfStrokes = 0;

        for (StrokeOverlayItem item : strokes) {
            totalNumberOfStrokes += item.getMultiplicity();
        }

        return totalNumberOfStrokes;
    }

    public Collection<? extends Stroke> getStrokes() {
        return strokes;
    }

    public int getIntervalDuration() {
        return intervalDuration;
    }

    public void setIntervalDuration(int intervalDuration) {
        this.intervalDuration = intervalDuration;
    }

    public int getIntervalOffset() {
        return intervalOffset;
    }

    public void setIntervalOffset(int intervalOffset) {
        this.intervalOffset = intervalOffset;
    }

    public int getRegion() {
        return region;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public void setReferenceTime(long referenceTime) {
        this.referenceTime = referenceTime;
    }

    public long getReferenceTime() {
        return referenceTime;
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