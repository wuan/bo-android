package org.blitzortung.android.map;

import android.util.Log;

import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.map.overlay.LayerOverlay;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public abstract class OwnMapActivity extends MapActivity {

    List<Overlay> overlays = new ArrayList<>();
    private OwnMapView mapView;

    public OwnMapView getMapView() {
        return mapView;
    }

    public void setMapView(OwnMapView mapView) {
        this.mapView = mapView;
    }

    public void addOverlay(Overlay overlay) {
        overlays.add(overlay);
    }

    public void updateOverlays() {
        List<Overlay> mapOverlays = mapView.getOverlays();

        mapOverlays.clear();

        for (Overlay overlay : overlays) {
            boolean enabled = true;

            if (overlay instanceof LayerOverlay) {
                enabled = ((LayerOverlay) overlay).isEnabled();
            }

            if (enabled) {
                mapOverlays.add(overlay);
            }
        }

        mapView.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            Field mConfigField = MapActivity.class.getDeclaredField("mConfig");
            mConfigField.setAccessible(true);

            Object mConfig = mConfigField.get(this);
            if (null != mConfig) {
                Field mConfigContextField = mConfig.getClass().getDeclaredField("context");
                mConfigContextField.setAccessible(true);
                mConfigContextField.set(mConfig, null);
                mConfigField.set(this, null);
            }
        } catch (Exception e) {
            Log.w(Main.LOG_TAG, "OwnMapActivity.onDestroy() failed");
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
