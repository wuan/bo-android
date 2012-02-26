package org.blitzortung.android.data.provider;

import java.util.List;

import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.Stroke;

public class DataResult {

	List<Stroke> strokes;
	List<Station> stations;
	
	boolean fail;
	
	boolean processWasLocked;
	
	public DataResult() {
		fail = true;
		
		processWasLocked = false;
	}
	
	public void setStrokes(List<Stroke> strokes) {
		this.strokes = strokes;
		fail = false;
	}
	
	public boolean containsStrokes() {
		return strokes != null;
	}
	public List<Stroke> getStrokes() {
		return strokes;
	}
	
	public void setStations(List<Station> stations) {
		this.stations = stations;
	}
	
	public boolean containsStations() {
		return stations != null;
	}
	
	public List<Station> getStations() {
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
}
