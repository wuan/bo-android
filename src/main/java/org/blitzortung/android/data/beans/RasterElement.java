package org.blitzortung.android.data.beans;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.Serializable;

public class RasterElement extends StrikeAbstract implements Serializable {

	private static final long serialVersionUID = 6765788323616893614L;
	
	private int multiplicity;
	
	public  RasterElement(RasterParameters rasterParameters, long referenceTimestamp, JSONArray jsonArray) {
		try {
			setLongitude(rasterParameters.getCenterLongitude(jsonArray.getInt(0)));
			setLatitude(rasterParameters.getCenterLatitude(jsonArray.getInt(1)));
			multiplicity = jsonArray.getInt(2);
			
			setTimestamp(referenceTimestamp + 1000 * jsonArray.getInt(3));
		} catch (JSONException e) {
			throw new RuntimeException("error with json format while parsing strike data", e);
		}
	}
	
	@Override
	public int getMultiplicity() {
		return multiplicity;
	}
}
