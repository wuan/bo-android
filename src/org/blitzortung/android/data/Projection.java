package org.blitzortung.android.data;

import com.google.android.maps.GeoPoint;

public class Projection {

	public static GeoPoint toMercator(float lon, float lat) {
//		double x = lon * 20037508.34 / 180;
//		double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
//		y = y * 20037508.34 / 180;

		return new GeoPoint((int) (lon * 1e6), (int) (lat * 1e6));
	}

	// public Point inverseMercator (float x, float y) {
	// double lon = (x / 20037508.34) * 180;
	// double lat = (y / 20037508.34) * 180;
	//
	// lat = 180/Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) -
	// Math.PI / 2);
	//
	// return new Point(lon, lat);
	// }

}
