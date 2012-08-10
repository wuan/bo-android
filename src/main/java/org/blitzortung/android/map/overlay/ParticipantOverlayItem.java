package org.blitzortung.android.map.overlay;

import org.blitzortung.android.data.Coordsys;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Participant.State;

import com.google.android.maps.OverlayItem;

public class ParticipantOverlayItem extends OverlayItem {

	final long lastDataTime;
	
	final State state;
	
	public ParticipantOverlayItem(Participant station) {
		super(Coordsys.toMapCoords(station.getLongitude(), station.getLatitude()), station.getName(), "");

		lastDataTime = station.getOfflineSince();
		state = station.getState();
	}
	
	public long getLastDataTime() {
		return lastDataTime;
	}
	
	public State getState() {
		return state;
	}

}
