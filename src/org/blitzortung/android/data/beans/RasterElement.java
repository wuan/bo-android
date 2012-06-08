package org.blitzortung.android.data.beans;

import org.json.JSONArray;
import org.json.JSONException;

public class RasterElement extends AbstractStroke {

	private int multiplicity;
	
	public  RasterElement(Raster raster, long referenceTimestamp, JSONArray jsonArray) {
		try {
			setLongitude(raster.getCenterLongitude(jsonArray.getInt(0)));
			setLatitude(raster.getCenterLatitude(jsonArray.getInt(1)));
			multiplicity = jsonArray.getInt(2);
			
			setTimestamp(referenceTimestamp + 1000 * jsonArray.getInt(3));
		} catch (JSONException e) {
			throw new RuntimeException("error with json format while parsing stroke data", e);
		}
	}
	
	@Override
	public int getMultiplicity() {
		return multiplicity;
	}
}
