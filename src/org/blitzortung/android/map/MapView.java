package org.blitzortung.android.map;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;

public class MapView extends com.google.android.maps.MapView {

	@SuppressWarnings("unused")
	private static final String TAG = "maps.MapView";

	public interface ZoomListener {
		public void onZoom(int zoomLevel);
	};
	
	List<ZoomListener> zoomListeners = new ArrayList<ZoomListener>();
		
	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public MapView(Context context, String apiKey) {
		super(context, apiKey);
	}

	int oldZoomLevel = -1;
	
	public void addZoomListener(ZoomListener zoomListener) {
		zoomListeners.add(zoomListener);
	}
	
}
