package org.blitzortung.android.map.overlay;

import com.google.android.maps.OverlayItem;
import org.blitzortung.android.data.Coordsys;
import org.blitzortung.android.data.beans.AbstractStroke;

public class StrokeOverlayItem extends OverlayItem {

	private final long timestamp;
	
	private final int multiplicity;
	
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
