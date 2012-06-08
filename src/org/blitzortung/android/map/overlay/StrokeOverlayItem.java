package org.blitzortung.android.map.overlay;

import org.blitzortung.android.data.Projection;
import org.blitzortung.android.data.beans.AbstractStroke;

import com.google.android.maps.OverlayItem;

public class StrokeOverlayItem extends OverlayItem {

	long timestamp;
	
	int multiplicity;
	
	public StrokeOverlayItem(AbstractStroke stroke) {
		super(Projection.toMapCoords(stroke.getLongitude(), stroke.getLatitude()), "", "");

		timestamp = stroke.getTimestamp();		
		
		multiplicity = stroke.getMultiplicity();
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public int getMultiplicity() {
		return multiplicity;
	}

}
