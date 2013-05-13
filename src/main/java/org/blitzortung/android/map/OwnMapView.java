package org.blitzortung.android.map;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import com.apple.eawt.event.GestureListener;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;

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

    private long firstTapDownTime;
    private int tapCount;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean result = super.onTouchEvent(event);

        detectAndHandleDoubleTap(event);
        detectAndHandleZoomAction();
		
		return result;
	}

    private void detectAndHandleDoubleTap(MotionEvent event) {
        long currentTime = System.currentTimeMillis();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentTime - firstTapDownTime < 400) {
                tapCount++;
            } else {
                firstTapDownTime = currentTime;
                tapCount = 1;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (tapCount == 2 && currentTime - firstTapDownTime < 400) {
                int x = (int)event.getX(), y = (int)event.getY();;
                Projection p = getProjection();
                getController().animateTo(p.fromPixels(x, y));
                getController().zoomIn();

                firstTapDownTime = 0;
            }
        }
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
        return zoomLevel - 1;
    }
}
