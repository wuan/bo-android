package org.blitzortung.android.data;

import com.google.android.maps.GeoPoint;

public class Projection {

	public static GeoPoint toMapCoords(float lon, float lat) {
		return new GeoPoint((int) (lon * 1e6), (int) (lat * 1e6));
	}

}
