package org.blitzortung.android.data.provider;

import java.util.ArrayList;
import java.util.List;

public class DataResult<E> {

	List<E> data;
	
	boolean fail;
	
	boolean processWasLocked;
	
	public DataResult() {
		data = new ArrayList<E>();
		
		fail = true;
		
		processWasLocked = false;
	}
	
	public void setData(List<E> data) {
		this.data = data;
		fail = false;
	}
	
	public List<E> getData() {
		return data;
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
