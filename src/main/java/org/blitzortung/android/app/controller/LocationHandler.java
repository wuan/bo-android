package org.blitzortung.android.app.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import org.blitzortung.android.app.view.PreferenceKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocationHandler implements SharedPreferences.OnSharedPreferenceChangeListener, LocationListener, GpsStatus.Listener {

    public static interface Listener {
        void onLocationChanged(Location location);
    }

    public static enum Provider {
        NETWORK(LocationManager.NETWORK_PROVIDER),
        GPS(LocationManager.GPS_PROVIDER),
        PASSIVE(LocationManager.PASSIVE_PROVIDER),
        MANUAL("manual");

        private String type;

        private Provider(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        private static Map<String, Provider> stringToValueMap = new HashMap<String, Provider>();

        static {
            for (Provider key : Provider.values()) {
                String keyString = key.getType();
                if (stringToValueMap.containsKey(keyString)) {
                    throw new IllegalStateException(String.format("key value '%s' already defined", keyString));
                }
                stringToValueMap.put(keyString, key);
            }
        }

        public static Provider fromString(String string) {
            return stringToValueMap.get(string);
        }
    }

    public void onPause() {
        locationManager.removeUpdates(this);
    }

    public void onResume() {
        enableProvider(provider);
    }

    private final LocationManager locationManager;

    private Provider provider;

    private final Location location;

    private Set<Listener> listeners = new HashSet<Listener>();

    public LocationHandler(Context context, SharedPreferences sharedPreferences) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this);
        location = new Location("");
        location.setLongitude(Double.NaN);
        location.setLatitude(Double.NaN);

        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.LOCATION_MODE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        this.location.set(location);
        sendLocationUpdate();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
        location.setLongitude(Double.NaN);
        location.setLatitude(Double.NaN);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case LOCATION_MODE:
                Provider newProvider = Provider.fromString(sharedPreferences.getString(key.toString(), Provider.NETWORK.toString()));
                if (newProvider != provider) {
                    updateProvider(newProvider, sharedPreferences);
                }
                break;

            case LOCATION_LONGITUDE:
                updateManualLongitude(sharedPreferences);
                break;

            case LOCATION_LATITUDE:
                updateManualLatitude(sharedPreferences);
                break;

        }
    }

    private void updateManualLatitude(SharedPreferences sharedPreferences) {
        try {
            location.setLatitude(Double.valueOf(sharedPreferences.getString(PreferenceKey.LOCATION_LATITUDE.toString(), "49.0")));
            sendLocationUpdate();
        } catch (NumberFormatException e) {
            Log.v("LocationHandler", "bad number format for manual latitude setting");
        }
    }

    private void updateManualLongitude(SharedPreferences sharedPreferences) {
        try {
            location.setLongitude(Double.valueOf(sharedPreferences.getString(PreferenceKey.LOCATION_LONGITUDE.toString(), "11.0")));
            sendLocationUpdate();
        } catch (NumberFormatException e) {
            Log.v("LocationHandler", "bad number format for manual longitude setting");
        }
    }

    private void updateProvider(Provider newProvider, SharedPreferences sharedPreferences) {
        if (newProvider == Provider.MANUAL) {
            locationManager.removeUpdates(this);
            updateManualLongitude(sharedPreferences);
            updateManualLatitude(sharedPreferences);
            location.setProvider(newProvider.getType());
        } else {
            invalidateLocation();
        }
        enableProvider(newProvider);
    }

    private void invalidateLocation() {
        location.setLongitude(Double.NaN);
        location.setLatitude(Double.NaN);
        sendUpdate(null);
    }

    private void enableProvider(Provider newProvider) {
        locationManager.removeUpdates(this);
        if (newProvider != null && newProvider != Provider.MANUAL) {
            locationManager.requestLocationUpdates(newProvider.getType(), 5000, 0, this);
        }
        provider = newProvider;
    }

    private void sendLocationUpdate() {
        sendUpdate(locationIsValid() ? location : null);
    }

    private void sendUpdate(Location location) {
        for (Listener listener : listeners) {
            listener.onLocationChanged(location);
        }
    }

    public void requestUpdates(LocationHandler.Listener target) {
        listeners.add(target);
        if (locationIsValid()) {
            target.onLocationChanged(location);
        }
    }

    private boolean locationIsValid() {
        return !Double.isNaN(location.getLongitude()) && !Double.isNaN(location.getLatitude());
    }

    public void removeUpdates(LocationHandler.Listener target) {
        listeners.remove(target);
    }

    @Override
    public void onGpsStatusChanged(int event) {

        if (provider == Provider.GPS) {
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    invalidateLocation();

                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc != null) {
                        long secondsElapsedSinceLastFix = (System.currentTimeMillis() - loc.getTime()) / 1000;

                        if (secondsElapsedSinceLastFix < 15) {
                            break;
                        }
                    }
                    if (locationIsValid()) {
                        invalidateLocation();
                    }
                    break;
            }
        }
    }

}
