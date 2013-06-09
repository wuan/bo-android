package org.blitzortung.android.app.controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.LocationManager;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.app.view.PreferenceKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class LocationHandlerTest {
    
    @Mock
    private Context context;
    
    @Mock
    private SharedPreferences sharedPreferences;
    
    @Mock
    private LocationManager locationManager;
    
    private LocationHandler locationHandler;
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
       
        when(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager);
        setLocationProviderPrefs(LocationManager.NETWORK_PROVIDER);
        
        locationHandler = new LocationHandler(context, sharedPreferences);
    }
    
    @Test
    public void testInitialization() {
        verify(sharedPreferences, times(1)).getString(PreferenceKey.LOCATION_MODE.toString(), LocationHandler.Provider.NETWORK.getType());
        verify(sharedPreferences, times(1)).registerOnSharedPreferenceChangeListener(locationHandler);
        verify(locationManager, times(1)).addGpsStatusListener(locationHandler);
        verify(locationManager, times(1)).removeUpdates(locationHandler);
        verify(locationManager, times(1)).requestLocationUpdates(LocationHandler.Provider.NETWORK.getType(), 10000, 10, locationHandler);       
    }
    
    private void setLocationProviderPrefs(String provider) {
        when(sharedPreferences.getString(PreferenceKey.LOCATION_MODE.toString(), LocationHandler.Provider.NETWORK.getType())).thenReturn(provider);
    }
}
