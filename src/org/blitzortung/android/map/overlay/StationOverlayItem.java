package org.blitzortung.android.map.overlay;

import java.util.Date;

import org.blitzortung.android.data.Projection;
import org.blitzortung.android.data.beans.Station;

import com.google.android.maps.OverlayItem;

public class StationOverlayItem extends OverlayItem {

	Date lastDataTime;
	
	public StationOverlayItem(Station station) {
		super(Projection.toMapCoords(station.getLatitude(), station.getLongitude()), "", "");

		lastDataTime = station.getLastDataTime();		
	}
	
	public Date getLastDataTime() {
		return lastDataTime;
	}

}
