package org.blitzortung.android.map.overlay;

import org.blitzortung.android.map.OwnMapActivity;

import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public abstract class AbstractOverlay<Item extends OverlayItem> extends ItemizedOverlay<Item> {

	private OwnMapActivity activity;
	
	public AbstractOverlay(OwnMapActivity activity, Drawable defaultMarker) {
		super(defaultMarker);
		this.activity = activity;
	}
	
	protected OwnMapActivity getActivity() {
		return activity;
	}

}
