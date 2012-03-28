package org.blitzortung.android.map.overlay;

import org.blitzortung.android.app.R;
import org.blitzortung.android.map.MapActivity;
import org.blitzortung.android.map.MapView;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public abstract class PopupOverlay<Item extends OverlayItem> extends AbstractOverlay<Item> {

	public PopupOverlay(MapActivity activity, Drawable defaultMarker) {
		super(activity, defaultMarker);
	}

	protected void showPopup(GeoPoint location, String text) {
		
		MapView map = getActivity().getMapView();
		
		View popUp = getActivity().getPopup();
		
		Log.v("PopupOverlay", "mapview: " + map);
		Log.v("PopupOverlay", "popup: " + popUp);
		
		map.removeView(popUp);

	    TextView statusText = (TextView) popUp.findViewById(R.id.popup_text);
	    statusText.setBackgroundColor(0x88000000);
	    statusText.setPadding(5,5,5,5);
	    statusText.setText(text);
	    
		MapView.LayoutParams mapParams = new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
		                        ViewGroup.LayoutParams.WRAP_CONTENT,
		                        location,
		                        0,
		                        0,
		                        MapView.LayoutParams.BOTTOM_CENTER);
		
		map.addView(popUp, mapParams);
	}
}
