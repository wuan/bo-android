package org.blitzortung.android.map.overlay;

import java.util.Date;

import org.blitzortung.android.data.Projection;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Station.State;

import com.google.android.maps.OverlayItem;

public class StationOverlayItem extends OverlayItem {

	Date lastDataTime;
	
	State state;
	
	public StationOverlayItem(Station station) {
		super(Projection.toMapCoords(station.getLongitude(), station.getLatitude()), station.getName(), "");

		lastDataTime = station.getOfflineSince();
		state = station.getState();
	}
	
	public Date getLastDataTime() {
		return lastDataTime;
	}
	
	public State getState() {
		return state;
	}

}
