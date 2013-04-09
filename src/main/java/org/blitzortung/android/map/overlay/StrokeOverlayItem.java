package org.blitzortung.android.map.overlay;

import android.location.Location;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;
import org.blitzortung.android.data.Coordsys;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Stroke;

public class StrokeOverlayItem extends OverlayItem implements Stroke {

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

    @Override
    public Location getLocation(Location location) {
        final GeoPoint point = getPoint();
        location.setLongitude(point.getLongitudeE6() / 1e6);
        location.setLatitude(point.getLatitudeE6() / 1e6);
        return location;
    }

    @Override
    public Location getLocation() {
        return getLocation(new Location("")); 
    }

    public int getMultiplicity() {
		return multiplicity;
	}

}
