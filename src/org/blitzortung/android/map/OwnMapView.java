package org.blitzortung.android.map;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.maps.MapView;

public class OwnMapView extends MapView {

	@SuppressWarnings("unused")
	private static final String TAG = "maps.MapView";

	public interface ZoomListener {
		public void onZoom(int zoomLevel);
	};
	
	List<ZoomListener> zoomListeners = new ArrayList<ZoomListener>();
		
	public OwnMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OwnMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public OwnMapView(Context context, String apiKey) {
		super(context, apiKey);
	}

	int oldZoomLevel = -1;
	
	public void addZoomListener(ZoomListener zoomListener) {
		zoomListeners.add(zoomListener);
	}
	
}
