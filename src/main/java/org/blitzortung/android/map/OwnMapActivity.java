package org.blitzortung.android.map;

import android.view.View;
import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;
import org.blitzortung.android.app.R;

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

    public void addOverlays(Overlay... overlays) {
        for (Overlay overlay : overlays) {
            mapView.getOverlays().add(overlay);
        }
    }
}
