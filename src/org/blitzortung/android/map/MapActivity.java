package org.blitzortung.android.map;

import org.blitzortung.android.app.R;

import android.view.View;

public abstract class MapActivity extends com.google.android.maps.MapActivity {

	private MapView mapView;
	
	private View popUp = null;
	
    public synchronized View getPopup() {
    	if (popUp == null) {
    		popUp = getLayoutInflater().inflate(R.layout.popup, mapView, false);
    	}
    	return popUp;
    }
    
    public void setMapView(MapView mapView) {
    	this.mapView = mapView;
    }
    
    public MapView getMapView() {
    	return mapView;
    }
}
