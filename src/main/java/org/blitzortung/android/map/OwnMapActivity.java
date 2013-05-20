package org.blitzortung.android.map;

import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;
import org.blitzortung.android.map.overlay.LayerOverlay;

import java.util.ArrayList;
import java.util.List;

public abstract class OwnMapActivity extends MapActivity {

	private OwnMapView mapView;
    
    List<Overlay> overlays = new ArrayList<Overlay>();
    
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
