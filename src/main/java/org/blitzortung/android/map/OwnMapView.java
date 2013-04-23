package org.blitzortung.android.map;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import com.google.android.maps.MapView;

import java.util.HashSet;
import java.util.Set;

public class OwnMapView extends MapView {

	private final Set<ZoomListener> zoomListeners = new HashSet<ZoomListener>();

	public interface ZoomListener {
		void onZoom(int zoomLevel);
	}

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

	protected void detectAndHandleZoomAction() {
        if (getProjection() != null) {
            float pixelSize = getProjection().metersToEquatorPixels(1000.0f);

            if (pixelSize != oldPixelSize) {
                notifyZoomListeners();
                oldPixelSize = pixelSize;
            }
        }
	}

	public void addZoomListener(ZoomListener zoomListener) {
		zoomListeners.add(zoomListener);
	}

	public void notifyZoomListeners() {
		for (ZoomListener zoomListener : zoomListeners) {
			zoomListener.onZoom(getZoomLevel());
		}
	}

    public int calculateTargetZoomLevel(float widthInMeters) {
        double equatorLength = 40075004; // in meters
        double widthInPixels = Math.min(getHeight(), getWidth());
        double metersPerPixel = equatorLength / 256;
        int zoomLevel = 1;
        while ((metersPerPixel * widthInPixels) > widthInMeters) {
            metersPerPixel /= 2;
            ++zoomLevel;
        }
        Log.i("BO_ANDROID", "zoom level = " + zoomLevel);
        return zoomLevel - 1;
    }
}
