package org.blitzortung.android.data.provider;

import java.io.Serializable;
import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Raster;

public class DataResult implements Serializable {

	private static final long serialVersionUID = -2104015890700948020L;

	private List<AbstractStroke> strokes;
	private List<Participant> stations;
	private Raster raster = null;
	
	private boolean fail;
	
	private boolean processWasLocked;
	
	private boolean incremental;
	
	public DataResult() {
		fail = true;
		
		processWasLocked = false;
		
		incremental = false;
	}
	
	public void setStrokes(List<AbstractStroke> strokes) {
		this.strokes = strokes;
		fail = false;
	}
	
	public boolean containsStrokes() {
		return strokes != null;
	}
	
	public List<AbstractStroke> getStrokes() {
		return strokes;
	}
	
	public void setStations(List<Participant> stations) {
		this.stations = stations;
	}
	
	public boolean containsParticipants() {
		return stations != null;
	}
	
	public List<Participant> getParticipants() {
		return stations;
	}
	
	public boolean retrievalWasSuccessful() {
		return !fail && !processWasLocked;
	}
	
	public boolean hasFailed() {
		return fail;
	}
	
	public void setProcessWasLocked() {
		this.processWasLocked = true;
	}
	
	public boolean processWasLocked() {
		return this.processWasLocked;
	}
	
	public boolean hasRaster() {
		return raster != null;
	}
	
	public Raster getRaster() {
		return raster;
	}
	
	public void setRaster(Raster raster) {
		this.raster = raster;
	}

	public boolean isIncremental() {
		return incremental;
	}
	
	public void setIncremental() {
		incremental = true;
	}
}
