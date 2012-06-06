package org.blitzortung.android.data;

import com.google.android.maps.GeoPoint;

public class Projection {

	public static GeoPoint toMapCoords(float longitude, float latitude) {
		return new GeoPoint((int) (latitude * 1e6), (int) (longitude * 1e6));
	}

}
