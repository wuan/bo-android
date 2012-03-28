package org.blitzortung.android.map.overlay;

import org.blitzortung.android.map.MapActivity;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public abstract class AbstractOverlay<Item extends OverlayItem> extends ItemizedOverlay<Item> {

	private MapActivity activity;
	
	public AbstractOverlay(MapActivity activity, Drawable defaultMarker) {
		super(defaultMarker);
		this.activity = activity;
	}
	
	protected MapActivity getActivity() {
		return activity;
	}

}
