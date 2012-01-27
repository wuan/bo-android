package org.blitzortung.android.app;

import java.util.Calendar;
import java.util.List;

import org.blitzortung.android.data.Provider;
import org.blitzortung.android.map.StrokesMapView;
import org.blitzortung.android.overlay.StrokesOverlay;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;

public class BOView extends MapActivity implements LocationListener {

	private static final String TAG = "Main";

	Location presentLocation;
	
	StrokesMapView mapView;
	
	TextView statusText; 

	StrokesOverlay strokesoverlay;
	
	int numberOfStrokes = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);

		mapView = (StrokesMapView) findViewById(R.id.mapview);
		
		mapView.setBuiltInZoomControls(true);

		statusText = (TextView) findViewById(R.id.status);

		strokesoverlay = new StrokesOverlay();

		numberOfStrokes = strokesoverlay.addStrokes(Provider.getStrokes());

		mapView.addZoomListener(new StrokesMapView.ZoomListener() {

			@Override
			public void onZoom(int zoomLevel) {
				strokesoverlay.updateShapeSize(1 + zoomLevel);
			}
			
		});

		List<Overlay> mapOverlays = mapView.getOverlays();
		mapOverlays.add(strokesoverlay);

		mapView.setSatellite(false);

	}

	private Handler mHandler = new Handler(); 
	
    private Runnable timerTask = new Runnable() {
    	
    	final int period = 60;
    	long lastUpdate = 0;
    	
        @Override 
        public void run() { 
            Calendar now = Calendar.getInstance();
            
            if ((now.getTimeInMillis() - lastUpdate) / 1000 >= period) {
				strokesoverlay.clear();
				numberOfStrokes = strokesoverlay.addStrokes(Provider.getStrokes());
				strokesoverlay.refresh();
				lastUpdate = now.getTimeInMillis();
				mapView.invalidate();
            }
            
            statusText.setText(String.format("%d/%d, %d strokes", 
                    (now.getTimeInMillis() - lastUpdate)/1000, 
                    period, 
                    numberOfStrokes));
            
            //Schedule the next update in one second
            mHandler.postDelayed(timerTask,1000); 
        } 
    };
    
    MenuDialog menuDialog; 
    private class MenuDialog extends AlertDialog { 
     
        public MenuDialog(Context context) { 
            super(context); 
            setTitle("Menu"); 
            View menu = getLayoutInflater().inflate(R.layout.menu, null); 
            setView(menu); 
        } 
     
        @Override 
        public boolean onKeyUp(int keyCode, KeyEvent event) { 
            if(keyCode == KeyEvent.KEYCODE_MENU) { 
                dismiss(); 
                return true; 
            } 
            return super.onKeyUp(keyCode, event); 
        } 
    } 
     
    @Override 
    public boolean onKeyUp(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_MENU) { 
            if(menuDialog == null) { 
                menuDialog = new MenuDialog(this); 
            } 
            menuDialog.show(); 
            return true; 
        } 
        return super.onKeyUp(keyCode, event); 
    } 
    
    @Override 
    public void onResume() { 
        super.onResume(); 
        mHandler.post(timerTask); 
    } 
     
    @Override 
    public void onPause() { 
        super.onPause(); 
        mHandler.removeCallbacks(timerTask); 
    } 
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {
		presentLocation = location;
		Log.d(TAG, "New location received");
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}

}