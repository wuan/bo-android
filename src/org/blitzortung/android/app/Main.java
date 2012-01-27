package org.blitzortung.android.app;

import java.util.List;

import org.blitzortung.android.data.Provider;
import org.blitzortung.android.map.StrokesMapView;
import org.blitzortung.android.overlay.StrokesOverlay;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class Main extends MapActivity implements LocationListener {

	private static final String TAG = "Main";

	Location presentLocation;

	StrokesOverlay strokesoverlay;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final StrokesMapView mapView = (StrokesMapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		strokesoverlay = new StrokesOverlay();

		strokesoverlay.addStrokes(Provider.getStrokes());

		mapView.addZoomListener(new StrokesMapView.ZoomListener() {

			@Override
			public void onZoom(int zoomLevel) {
				strokesoverlay.updateShapeSize(1 + zoomLevel);
			}
			
		});

		List<Overlay> mapOverlays = mapView.getOverlays();
		mapOverlays.add(strokesoverlay);

		mapView.setSatellite(false);

		final Button button = (Button) findViewById(R.id.button1);

		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				MapView mapView = (MapView) findViewById(R.id.mapview);

				strokesoverlay.clear();
				strokesoverlay.addStrokes(Provider.getStrokes());
				strokesoverlay.refresh();

				mapView.invalidate();
			}
		});

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