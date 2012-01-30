package org.blitzortung.android.app;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.blitzortung.android.data.Credentials;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.Provider;
import org.blitzortung.android.data.beans.Stroke;
import org.blitzortung.android.map.StrokesMapView;
import org.blitzortung.android.map.overlay.StrokesOverlay;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.maps.MapActivity;
import com.google.android.maps.Overlay;

public class Main extends MapActivity implements LocationListener, DataListener {

	private static final String TAG = "Main";

	Location presentLocation;
	
	StrokesMapView mapView;
	
	TextView statusText; 

	Provider provider;
	
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

		provider = new Provider(new Credentials("asfd", "adsf"),  (ProgressBar) findViewById(R.id.progress), this);
		
		//provider.updateStrokes();

		mapView.addZoomListener(new StrokesMapView.ZoomListener() {

			@Override
			public void onZoom(int zoomLevel) {
				strokesoverlay.updateShapeSize(1 + zoomLevel);
			}
			
		});

		List<Overlay> mapOverlays = mapView.getOverlays();
		mapOverlays.add(strokesoverlay);

		mapView.setSatellite(false);

		mapView.invalidate();
	}

	private Handler mHandler = new Handler(); 
	
    private Runnable timerTask = new Runnable() {
    	
    	int period = 20;
    	long lastUpdate = 0;
    	
        @Override 
        public void run() { 
            Calendar now = Calendar.getInstance();
            
            if ((now.getTimeInMillis() - lastUpdate) / 1000 >= period) {
            	Log.v(TAG, "update data");
				provider.updateStrokes();
				lastUpdate = now.getTimeInMillis();
            }
            
            statusText.setText(String.format("%d/%d, %d strokes", 
                    (now.getTimeInMillis() - lastUpdate)/1000, 
                    period, 
                    numberOfStrokes));
            
            //Schedule the next update in one second
            mHandler.postDelayed(timerTask,1000); 
        }
        
        public void setPeriod(int period) {
        	this.period = period;
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
        Log.v(TAG, "onResume()");
        mHandler.post(timerTask); 
    } 
     
    @Override 
    public void onPause() { 
        super.onPause();
        Log.v(TAG, "onPause()");
        mHandler.removeCallbacks(timerTask); 
    } 
    
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onLocationChanged(Location location) {
		presentLocation = location;
		Log.v(TAG, "New location received");
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

	@Override
	public void onStrokeDataArrival(List<Stroke> strokes) {
		Log.v(TAG, String.format("received %d strokes", strokes.size()));
		numberOfStrokes = strokesoverlay.addStrokes(strokes);
		Calendar expireTime = new GregorianCalendar();
		expireTime.add(Calendar.MINUTE, -60);
		
		strokesoverlay.expireStrokes(expireTime.getTime());
		strokesoverlay.refresh();
		mapView.invalidate();
	}

}