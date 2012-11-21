package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.format.DateFormat;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;
import org.blitzortung.android.data.TimeIntervalWithOffset;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

import java.util.ArrayList;
import java.util.List;

public class StrokesOverlay extends PopupOverlay<StrokeOverlayItem> implements TimeIntervalWithOffset{

    @SuppressWarnings("unused")
    private static final String TAG = "overlay.StrokesOverlay";

    // VisibleForTesting
    protected final ArrayList<StrokeOverlayItem> items;

    private final StrokeColorHandler colorHandler;

    private int zoomLevel;

    RasterParameters rasterParameters = null;

    static private final Drawable DefaultDrawable;

    private int intervalDuration;

    private int intervalOffset;

    private int region;

    private long referenceTime;

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

            if (getRasterParameters()) {
                Paint paint = new Paint();
                paint.setColor(colorHandler.getLineColor());
                paint.setStyle(Style.STROKE);
                Projection projection = mapView.getProjection();
                canvas.drawRect(rasterParameters.getRect(projection), paint);
            }
        }
    }

    public void addStrokes(List<AbstractStroke> strokes) {
        if (getRasterParameters()) {
            items.clear();
        }

        for (AbstractStroke stroke : strokes) {
            items.add(new StrokeOverlayItem(stroke));
        }

        if (!getRasterParameters()) {
            long expireTime = System.currentTimeMillis() - intervalDuration * 60 * 1000;
            expireStrokes(expireTime);
        }

        setLastFocusedIndex(-1);
        populate();
    }

    // VisibleForTesting
    protected void expireStrokes(long expireTime) {
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

    public ColorHandler getColorHandler() {
        return colorHandler;
    }

    public void refresh() {
        long now = System.currentTimeMillis();

        int current_section = -1;

        colorHandler.updateTarget();

        Drawable drawable = null;

        for (StrokeOverlayItem item : items) {
            int section = colorHandler.getColorSection(now, item.getTimestamp(), this);

            if (getRasterParameters() || current_section != section) {
                drawable = getDrawable(item, section, colorHandler);
            }

            item.setMarker(drawable);
        }
    }

    // VisibleForTesting
    protected Drawable getDrawable(StrokeOverlayItem item, int section, ColorHandler colorHandler) {

        Shape shape;

        Projection projection = getActivity().getMapView().getProjection();

        int color = colorHandler.getColor(section);

        if (getRasterParameters()) {
            float lon_delta = rasterParameters.getLongitudeDelta() / 2.0f * 1e6f;
            float lat_delta = rasterParameters.getLatitudeDelta() / 2.0f * 1e6f;
            GeoPoint geoPoint = item.getPoint();
            Point center = projection.toPixels(geoPoint, null);
            Point topLeft = projection.toPixels(new GeoPoint(
                    (int) (geoPoint.getLatitudeE6() + lat_delta),
                    (int) (geoPoint.getLongitudeE6() - lon_delta)), null);
            Point bottomRight = projection.toPixels(new GeoPoint(
                    (int) (geoPoint.getLatitudeE6() - lat_delta),
                    (int) (geoPoint.getLongitudeE6() + lon_delta)), null);
            topLeft.offset(-center.x, -center.y);
            bottomRight.offset(-center.x, -center.y);
            shape = new RasterShape(topLeft, bottomRight, color, item.getMultiplicity(), colorHandler.getTextColor());
        } else {
            shape = new StrokeShape(zoomLevel + 1, color);

        }
        return new ShapeDrawable(shape);
    }

    public void setRasterParameters(RasterParameters rasterParameters) {
        this.rasterParameters = rasterParameters;
    }

    public boolean getRasterParameters() {
        return rasterParameters != null;
    }

    public boolean hasRealtimeData() {
         return intervalOffset == 0;
    }

    @Override
    protected boolean onTap(int index) {
        StrokeOverlayItem item = items.get(index);
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

        for (StrokeOverlayItem item : items) {
            totalNumberOfStrokes += item.getMultiplicity();
        }

        return totalNumberOfStrokes;
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
}