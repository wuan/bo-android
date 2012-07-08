package org.blitzortung.android.map.overlay;

import org.blitzortung.android.data.Coordsys;
import org.blitzortung.android.data.beans.AbstractStroke;

import com.google.android.maps.OverlayItem;

public class StrokeOverlayItem extends OverlayItem {

	private long timestamp;
	
	private int multiplicity;
	
	public StrokeOverlayItem(AbstractStroke stroke) {
		super(Coordsys.toMapCoords(stroke.getLongitude(), stroke.getLatitude()), "", "");

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
