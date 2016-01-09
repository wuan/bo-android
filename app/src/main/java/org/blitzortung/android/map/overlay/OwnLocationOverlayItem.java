package org.blitzortung.android.map.overlay;

import android.location.Location;
import com.google.android.maps.OverlayItem;
import org.blitzortung.android.data.Coordsys;

public class OwnLocationOverlayItem extends OverlayItem {

	private final float radius;
	
	public OwnLocationOverlayItem(Location location, float radius) {
		super(Coordsys.toMapCoords((float)location.getLongitude(), (float)location.getLatitude()), "", "");
		
		this.radius = radius;
	}
	
	public float getRadius() {
		return radius;
	}

}
