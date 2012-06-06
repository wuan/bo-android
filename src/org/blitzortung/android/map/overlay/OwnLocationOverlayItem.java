package org.blitzortung.android.map.overlay;

import org.blitzortung.android.data.Projection;

import android.location.Location;

import com.google.android.maps.OverlayItem;

public class OwnLocationOverlayItem extends OverlayItem {

	private float radius;
	
	public OwnLocationOverlayItem(Location location, float radius) {
		super(Projection.toMapCoords((float)location.getLongitude(), (float)location.getLatitude()), "", "");
		
		this.radius = radius;
	}
	
	public float getRadius() {
		return radius;
	}

}
