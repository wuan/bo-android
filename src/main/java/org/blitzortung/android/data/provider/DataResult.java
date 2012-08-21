package org.blitzortung.android.data.provider;

import java.io.Serializable;
import java.util.List;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Raster;

public class DataResult implements Serializable {

	private static final long serialVersionUID = -2104015890700948020L;

	private List<AbstractStroke> strokes;
	private List<Participant> participants;
	private Raster raster = null;
	
	private boolean fail;
	
	private boolean processWasLocked;
	
	private boolean incremental;
    private long strokesTimeInterval;

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
	
	public void setParticipants(List<Participant> participants) {
		this.participants = participants;
	}
	
	public boolean containsParticipants() {
		return participants != null;
	}
	
	public List<Participant> getParticipants() {
		return participants;
	}
	
	public boolean retrievalWasSuccessful() {
		return !fail && !processWasLocked;
	}
	
	public boolean hasFailed() {
		return fail;
	}
	
	public void setProcessWasLocked() {
		processWasLocked = true;
	}
	
	public boolean processWasLocked() {
		return processWasLocked;
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

    public void setStrokesTimeInterval(long strokesTimeInterval) {
        this.strokesTimeInterval = strokesTimeInterval;
    }

    public long getStrokesTimeInterval() {
        return strokesTimeInterval;
    }
}
