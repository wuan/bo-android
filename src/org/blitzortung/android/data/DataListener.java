package org.blitzortung.android.data;

import java.util.List;

import org.blitzortung.android.data.beans.Stroke;

public interface DataListener {

	public void onStrokeDataArrival(List<Stroke> strokes);
	
	public void onStrokeDataReset();
}
