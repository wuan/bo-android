package org.blitzortung.android.map;

import org.blitzortung.android.app.R;

import android.view.View;

import com.google.android.maps.MapActivity;

public abstract class OwnMapActivity extends MapActivity {

	private OwnMapView mapView;
	
	private View popUp = null;
	
    public synchronized View getPopup() {
    	if (popUp == null) {
    		popUp = getLayoutInflater().inflate(R.layout.popup, mapView, false);
    	}
    	return popUp;
    }
    
    public void setMapView(OwnMapView mapView) {
    	this.mapView = mapView;
    }
    
    public OwnMapView getMapView() {
    	return mapView;
    }
}
