package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.text.format.DateFormat;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.data.Parameters;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Strike;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.components.LayerOverlayComponent;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrikeColorHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StrikesOverlay extends PopupOverlay<StrikeOverlayItem> implements LayerOverlay {

    static private final Drawable DEFAULT_DRAWABLE;

    static {
        StrikeShape shape = new StrikeShape();
        shape.update(1, 0);
        DEFAULT_DRAWABLE = new ShapeDrawable(shape);
    }

    // VisibleForTesting
    protected final ArrayList<StrikeOverlayItem> strikes;
    private final StrikeColorHandler colorHandler;
    private final LayerOverlayComponent layerOverlayComponent;
    private int zoomLevel;
    private RasterParameters rasterParameters = null;
    private long referenceTime;
    private Parameters parameters = Parameters.DEFAULT;

    public StrikesOverlay(OwnMapActivity mapActivity, StrikeColorHandler colorHandler) {
        super(mapActivity, boundCenter(DEFAULT_DRAWABLE));

        layerOverlayComponent = new LayerOverlayComponent(mapActivity.getResources().getString(R.string.strikes_layer));
        this.colorHandler = colorHandler;

        strikes = new ArrayList<>();

        populate();
    }

    @Override
    protected StrikeOverlayItem createItem(int index) {
        return strikes.get(index);
    }

    @Override
    public int size() {
        return strikes.size();
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
        RectF rect = getRasterParameters().getRect(mapView.getProjection());

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

    public void addStrikes(List<StrikeAbstract> strikes) {
        Log.v(Main.LOG_TAG, "StrikesOverlay.addStrikes() #" + strikes.size());
        this.strikes.addAll(
                Stream.of(strikes)
                        .map(StrikeOverlayItem::new)
                        .collect(Collectors.toList()));
        setLastFocusedIndex(-1);
        populate();
    }

    public void expireStrikes() {
        long expireTime = referenceTime - (parameters.getIntervalDuration() - parameters.getIntervalOffset()) * 60 * 1000;

        final List<StrikeOverlayItem> toRemove = Stream.of(strikes)
                .filter(s -> s.getTimestamp() < expireTime)
                .collect(Collectors.toList());

        if (toRemove.size() > 0) {
            strikes.removeAll(toRemove);
        }
    }

    public void clear() {
        setLastFocusedIndex(-1);
        clearPopup();
        strikes.clear();
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

        int current_section = -1;

        colorHandler.updateTarget();

        Shape drawable = null;

        for (StrikeOverlayItem item : strikes) {
            int section = colorHandler.getColorSection(
                    hasRealtimeData()
                            ? System.currentTimeMillis()
                            : referenceTime,
                    item.getTimestamp(), parameters);

            if (hasRasterParameters() || current_section != section) {
                drawable = updateAndReturnDrawable(item, section, colorHandler);
            } else {
                item.setShape(drawable);
            }
        }
    }

    // VisibleForTesting
    protected Shape updateAndReturnDrawable(StrikeOverlayItem item, int section, ColorHandler colorHandler) {
        final Projection projection = getActivity().getMapView().getProjection();
        final int color = colorHandler.getColor(section);
        final int textColor = colorHandler.getTextColor();

        item.updateShape(getRasterParameters(), projection, color, textColor, zoomLevel);

        return item.getShape();
    }

    public boolean hasRasterParameters() {
        return rasterParameters != null;
    }

    public RasterParameters getRasterParameters() {
        return rasterParameters;
    }

    public void setRasterParameters(RasterParameters rasterParameters) {
        this.rasterParameters = rasterParameters;
    }

    public boolean hasRealtimeData() {
        return parameters.isRealtime();
    }

    @Override
    protected boolean onTap(int index) {
        StrikeOverlayItem item = strikes.get(index);
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

    public int getTotalNumberOfStrikes() {
        int totalNumberOfStrikes = 0;

        for (StrikeOverlayItem item : strikes) {
            totalNumberOfStrikes += item.getMultiplicity();
        }

        return totalNumberOfStrikes;
    }

    public Collection<? extends Strike> getStrikes() {
        return strikes;
    }

    public long getReferenceTime() {
        return referenceTime;
    }

    public void setReferenceTime(long referenceTime) {
        this.referenceTime = referenceTime;
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

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}