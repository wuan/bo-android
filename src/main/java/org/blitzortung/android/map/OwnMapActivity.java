package org.blitzortung.android.map;

import android.view.View;
import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.overlay.LayerOverlay;

import java.util.ArrayList;
import java.util.List;

public abstract class OwnMapActivity extends MapActivity {

	private OwnMapView mapView;
	
	private View popUp = null;
    
    List<Overlay> overlays = new ArrayList<Overlay>();
	
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

    public void addOverlays(Overlay... addedOverlays) {
        for (Overlay overlay : addedOverlays) {
            overlays.add(overlay);
        }
        updateOverlays();
    }
    
    public void updateOverlays()
    {
        List<Overlay> mapOverlays = mapView.getOverlays();
        
        mapOverlays.clear();
        
        for (Overlay overlay : overlays) {
            boolean enabled = true;
            
            if (overlay instanceof LayerOverlay) {
                enabled = ((LayerOverlay)overlay).isEnabled();
            }
            
            if (enabled) {
                mapOverlays.add(overlay);
            }
        } 
        
        mapView.invalidate();
    }
}
