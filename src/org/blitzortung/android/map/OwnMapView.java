package org.blitzortung.android.map;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class OwnMapView extends MapView {

	Set<ZoomListener> zoomListeners = new HashSet<ZoomListener>();

	public interface ZoomListener {
		public void onZoom(int zoomLevel);
	};

	public OwnMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OwnMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public OwnMapView(Context context, String apiKey) {
		super(context, apiKey);
	}

	private float oldPixelSize = -1;

	@Override
	public void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		detectAndHandleZoomAction();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean result = super.onTouchEvent(event);
		detectAndHandleZoomAction();
		return result;
	}

	private void detectAndHandleZoomAction() {
		float pixelSize = getProjection().metersToEquatorPixels(1000.0f);

		if (pixelSize != oldPixelSize) {
			notifyZoomListeners(getZoomLevel());
			oldPixelSize = pixelSize;
		}
	}

	public void addZoomListener(ZoomListener zoomListener) {
		zoomListeners.add(zoomListener);
	}

	public void notifyZoomListeners(int level) {
		for (ZoomListener zoomListener : zoomListeners) {
			zoomListener.onZoom(getZoomLevel());
		}
	}
}
