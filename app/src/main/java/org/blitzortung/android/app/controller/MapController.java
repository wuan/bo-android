package org.blitzortung.android.app.controller;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.component.DataComponent;
import org.blitzortung.android.data.component.StrokesComponent;
import org.blitzortung.android.data.component.TimeComponent;
import org.blitzortung.android.map.color.ColorHandler;

public class MapController implements GoogleMap.OnCameraChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final float EQUATOR_LENGTH = 40075004;

    private final Resources resources;
    private final MapFragment mapFragment;
    private final StrokesComponent strokesComponent;
    private final GoogleMap map;

    public MapController(MapFragment mapFragment, SharedPreferences preferences, Resources resources, StrokesComponent strokesComponent) {
        this.mapFragment = mapFragment;
        map = mapFragment.getMap();
        this.resources = resources;
        this.strokesComponent = strokesComponent;

        initializeMap();

        onSharedPreferenceChanged(preferences, PreferenceKey.MAP_TYPE, PreferenceKey.MAP_FADE, PreferenceKey.SHOW_LOCATION, PreferenceKey.SHOW_PARTICIPANTS);
    }

    private void initializeMap() {
        map.setOnCameraChangeListener(this);
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        map.setMyLocationEnabled(false);
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setTiltGesturesEnabled(false);
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setCompassEnabled(false);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        Log.v(Main.LOG_TAG, String.format("MapController.onCameraChange() zoom: %f, target: %s", cameraPosition.zoom, cameraPosition.target.toString()));
    }

    public void updateMap(DataComponent data, TimeComponent time, RasterParameters rasterParameters, ColorHandler colorHandler) {

        float width = rasterParameters.getLongitudeDelta();
        float height = rasterParameters.getLatitudeDelta();

        map.clear();

        addRasterDataArea(rasterParameters);

        for (AbstractStroke stroke : data.getStrokes()) {
            float longitude = stroke.getLongitude();
            float latitude = stroke.getLatitude();

            int color = colorHandler.getColor(time.getReferenceTime(), stroke.getTimestamp(), time.getIntervalDuration());

            addGroundOverlay(longitude, latitude, width, height, color, 0f);
        }
    }

    private void addRasterDataArea(RasterParameters rasterParameters) {
        map.addPolygon(new PolygonOptions()
                .add(
                        new LatLng(rasterParameters.getMaxLatitude(), rasterParameters.getMinLongitude()),
                        new LatLng(rasterParameters.getMaxLatitude(), rasterParameters.getMaxLongitude()),
                        new LatLng(rasterParameters.getMinLatitude(), rasterParameters.getMaxLongitude()),
                        new LatLng(rasterParameters.getMinLatitude(), rasterParameters.getMinLongitude())
                )
                .strokeColor(Color.WHITE)
                .strokeWidth(1f)
                .fillColor(Color.TRANSPARENT));
    }

    private void addGroundOverlay(float longitude, float latitude, float width, float height, int color, float transparency) {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        bitmap.setPixel(0, 0, color);
        BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(bitmap);
        LatLngBounds bounds = new LatLngBounds(
                new LatLng(latitude - height / 2, longitude - width / 2),
                new LatLng(latitude + height / 2, longitude + width / 2)
        );
        map.addGroundOverlay(new GroundOverlayOptions()
                .image(image)
                .positionFromBounds(bounds)
                .transparency(transparency)
        );
    }

    public void animateTo(float longitude, float latitude, float diameter) {

        DisplayMetrics metrics = resources.getDisplayMetrics();
        float mapWidth = mapFragment.getView().getWidth() / metrics.scaledDensity;

        float zoom = getZoomForMetersWide(diameter * 1000, mapWidth, latitude);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom);
        map.animateCamera(cameraUpdate);
    }

    public static float getZoomForMetersWide (
            final float desiredMeters,
            final float mapWidth,
            final float latitude )
    {
        final float latitudinalAdjustment = (float) Math.cos( Math.PI * latitude / 180.0f );

        final float arg = EQUATOR_LENGTH * mapWidth * latitudinalAdjustment / ( desiredMeters * 256.0f );

        return (float) (Math.log( arg ) / Math.log( 2.0 ));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey... keys) {
        for (PreferenceKey key : keys) {
            onSharedPreferenceChanged(sharedPreferences, key);
        }
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case MAP_TYPE:
                String mapTypeString = sharedPreferences.getString(key.toString(), "SATELLITE");
                map.setMapType(mapTypeString.equals("SATELLITE") ? GoogleMap.MAP_TYPE_SATELLITE : GoogleMap.MAP_TYPE_NORMAL);
                break;

            case SHOW_PARTICIPANTS:
                boolean showParticipants = sharedPreferences.getBoolean(key.toString(), true);
                // TODO refresh map
                break;

            case MAP_FADE:
                int alphaValue = Math.round(255.0f / 100.0f * sharedPreferences.getInt(key.toString(), 40));
                // TODO
                break;
        }
    }
}
