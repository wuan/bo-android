package org.blitzortung.android.overlay;

import java.util.Date;

import org.blitzortung.android.data.Projection;
import org.blitzortung.android.data.Stroke;

import com.google.android.maps.OverlayItem;

public class StrokeOverlayItem extends OverlayItem {

	Date timestamp;
	
	public StrokeOverlayItem(Stroke stroke) {
		super(Projection.toMercator(stroke.getLatitude(), stroke.getLongitude()), "", "");

		timestamp = stroke.getTime();		
	}
	
	public Date getTimestamp() {
		return timestamp;
	}

}
