package org.blitzortung.android.map.overlay;

import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.map.OwnMapView;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.maps.ItemizedOverlay;

public class OwnLocationOverlay extends ItemizedOverlay<OwnLocationOverlayItem> implements LocationListener {

	static private Drawable DEFAULT_DRAWABLE;
	static {
		Shape shape = new OwnLocationShape(1);
		DEFAULT_DRAWABLE = new ShapeDrawable(shape);
	}
	
	private OwnLocationOverlayItem item;
	
	private LocationManager locationManager;
	
	private int zoomLevel;
	
	public OwnLocationOverlay(Context context, OwnMapView mapView) {
		super(DEFAULT_DRAWABLE);
		
		item = null;
		
		populate();
		
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		
		mapView.addZoomListener(new OwnMapView.ZoomListener() {

			@Override
			public void onZoom(int newZoomLevel) {
				zoomLevel = newZoomLevel;
				refresh();
			}

		});
		
		zoomLevel = mapView.getZoomLevel();
		
		mapView.getOverlays().add(this);
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences.getBoolean(Preferences.SHOW_LOCATION_KEY, false)) {
			enableOwnLocation();
		}
		
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

	@Override
	public void onLocationChanged(Location location) {
		item = new OwnLocationOverlayItem(location, 25000);
		
		populate();
		refresh();
	}

	@Override
	public void onProviderDisabled(String arg0) {
		item = null;
		
		populate();
	}

	@Override
	public void onProviderEnabled(String arg0) {	
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}

	public void enableOwnLocation() {	
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	}

	public void disableOwnLocation() {
		locationManager.removeUpdates(this);
	}

}
