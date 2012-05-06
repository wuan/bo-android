package org.blitzortung.android.map;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

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

	private int oldZoomLevel = -1;

	private float oldPixelSize = -1;

	@Override
	public void computeScroll() {
		super.computeScroll();
		
		float pixelSize = getProjection().metersToEquatorPixels(1000.0f);

		if (getZoomLevel() != oldZoomLevel) {

			if (oldPixelSize == pixelSize) {
				for (ZoomListener zoomListener : zoomListeners)
					zoomListener.onZoom(getZoomLevel());
				oldPixelSize = -1;
				oldZoomLevel = getZoomLevel();
			} else {
				oldPixelSize = pixelSize;
			}

		}
	}

	public void addZoomListener(ZoomListener zoomListener) {
		Log.v("OwnMapView", "add zoom listener: " + zoomListener.toString());
		zoomListeners.add(zoomListener);
	}

}
