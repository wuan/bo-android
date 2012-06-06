package org.blitzortung.android.map;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.google.android.maps.MapView;

public class OwnMapView extends MapView {

	@SuppressWarnings("unused")
	private static final String TAG = "maps.MapView";

	public interface ZoomListener {
		public void onZoom(int zoomLevel);
	};

	Set<ZoomListener> zoomListeners = new HashSet<ZoomListener>();

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
	public void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		float pixelSize = getProjection().metersToEquatorPixels(1000.0f);

		if (getZoomLevel() != oldZoomLevel) {

			for (ZoomListener zoomListener : zoomListeners) {
				zoomListener.onZoom(getZoomLevel());
			}

			if (oldPixelSize == pixelSize) {
				oldPixelSize = -1;
				oldZoomLevel = getZoomLevel();
			} else {
				oldPixelSize = pixelSize;
			}
		}
	}

	public void addZoomListener(ZoomListener zoomListener) {
		zoomListeners.add(zoomListener);
	}

}
