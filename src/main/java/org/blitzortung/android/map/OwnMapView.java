package org.blitzortung.android.map;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import org.blitzortung.android.app.R;

import java.util.HashSet;
import java.util.Set;

public class OwnMapView extends MapView {

	private final Set<ZoomListener> zoomListeners = new HashSet<ZoomListener>();

    private View popUp = null;
    
    private GestureDetector gestureDetector;

    public interface ZoomListener {
		void onZoom(int zoomLevel);
	}

    public OwnMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OwnMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

	public OwnMapView(Context context, String apiKey) {
		super(context, apiKey);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent event) {
                removeView(getPopup());

                int x = (int)event.getX(), y = (int)event.getY();;
                Projection p = getProjection();
                getController().animateTo(p.fromPixels(x, y));
                getController().zoomIn();
                return true;
            }
        });
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

        gestureDetector.onTouchEvent(event);
        
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
        return zoomLevel - 1;
    }

    public synchronized View getPopup() {
        if (popUp == null) {
            popUp = LayoutInflater.from(getContext()).inflate(R.layout.popup, this, false);
        }
        return popUp;
    }
}
