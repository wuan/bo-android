package org.blitzortung.android.map.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.location.Location;
import android.preference.PreferenceManager;

import com.annimon.stream.function.Consumer;
import com.google.android.maps.ItemizedOverlay;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.location.LocationEvent;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.components.LayerOverlayComponent;

public class OwnLocationOverlay extends ItemizedOverlay<OwnLocationOverlayItem> implements SharedPreferences.OnSharedPreferenceChangeListener, LayerOverlay {

    static private final Drawable DEFAULT_DRAWABLE;

    static {
        Shape shape = new OwnLocationShape(1);
        DEFAULT_DRAWABLE = new ShapeDrawable(shape);
    }

    private final LayerOverlayComponent layerOverlayComponent;

    private OwnLocationOverlayItem item;

    private int zoomLevel;

    public OwnLocationOverlay(Context context, OwnMapView mapView) {
        super(DEFAULT_DRAWABLE);

        layerOverlayComponent = new LayerOverlayComponent(context.getResources().getString(R.string.own_location_layer));

        item = null;

        populate();

        mapView.addZoomListener(newZoomLevel -> {
            if (newZoomLevel != zoomLevel) {
                zoomLevel = newZoomLevel;
                refresh();
            }
        });

        zoomLevel = mapView.getZoomLevel();

        mapView.getOverlays().add(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_LOCATION.toString());

        refresh();
    }

    @Override
    public void draw(Canvas canvas, com.google.android.maps.MapView mapView, boolean shadow) {
        if (!shadow) {
            super.draw(canvas, mapView, false);
        }
    }

    private void refresh() {
        if (item != null) {
            item.setMarker(new ShapeDrawable(new OwnLocationShape(zoomLevel + 1)));
        }
    }

    @Override
    protected OwnLocationOverlayItem createItem(int i) {
        return item;
    }

    @Override
    public int size() {
        return item == null ? 0 : 1;
    }

    private Consumer<LocationEvent> locationEventConsumer = new Consumer<LocationEvent>() {
        @Override
        public void accept(LocationEvent event) {
            Location location = event.getLocation();

            item = location != null ? new OwnLocationOverlayItem(location, 25000) : item;

            populate();
            refresh();
        }
    };

    public Consumer<LocationEvent> getLocationEventConsumer() {
        return locationEventConsumer;
    }

    public void enableOwnLocation() {
    }

    public void disableOwnLocation() {
        item = null;
        populate();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        if (key == PreferenceKey.SHOW_LOCATION) {
            boolean showLocation = sharedPreferences.getBoolean(key.toString(), false);

            if (showLocation) {
                enableOwnLocation();
            } else {
                disableOwnLocation();
            }
        }
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
