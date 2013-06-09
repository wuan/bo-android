package org.blitzortung.android.map.overlay;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.preference.PreferenceManager;
import com.google.android.maps.Overlay;
import com.google.common.collect.Lists;
import org.blitzortung.android.app.controller.LocationHandler;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.map.OwnMapView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowPreferenceManager;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class OwnLocationOverlayTest {

    private OwnLocationOverlay ownLocationOverlay;

    @Mock
    private Context context;

    @Mock
    private Resources resources;

    @Mock
    private OwnMapView mapView;

    @Mock
    private LocationHandler locationHandler;

    private SharedPreferences sharedPreferences;

    private final List<Overlay> overlays = Lists.newArrayList();

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application);

        when(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationHandler);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(context.getResources()).thenReturn(resources);
        when(context.getApplicationContext()).thenReturn(Robolectric.application);
        when(mapView.getOverlays()).thenReturn(overlays);

        ownLocationOverlay = new OwnLocationOverlay(context, locationHandler, mapView);
    }

    @Test
    public void testConstruct()
    {
        verify(mapView, times(1)).addZoomListener(any(OwnMapView.ZoomListener.class));
        verify(mapView, times(1)).getZoomLevel();

        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem((Overlay)ownLocationOverlay));
    }

    @Test
    public void testSize()
    {
        assertThat(ownLocationOverlay.size(), is(0));

        enableOwnLocation();

        assertThat(ownLocationOverlay.size(), is(0));

        updateLocation();

        assertThat(ownLocationOverlay.size(), is(1));
    }

    @Test
    public void testCreateItem()
    {
        assertThat(ownLocationOverlay.createItem(0), is(nullValue()));

        enableOwnLocation();

        assertThat(ownLocationOverlay.createItem(0), is(nullValue()));

        updateLocation();

        assertThat(ownLocationOverlay.createItem(0), is(OwnLocationOverlayItem.class));
    }

    @Test
    public void testOnLocationChanged()
    {

        ownLocationOverlay.onLocationChanged(mock(Location.class));
        
        assertThat(ownLocationOverlay.size(), is(1));
    }
    
    @Test
    public void testOnLocationChangedWithNullLocation()
    {
        ownLocationOverlay.onLocationChanged(null);
        
        assertThat(ownLocationOverlay.size(), is(0));
    }

    @Test
    public void testDisableOwnLocation()
    {
        ownLocationOverlay.onLocationChanged(mock(Location.class));
        
        ownLocationOverlay.disableOwnLocation();

        assertThat(ownLocationOverlay.size(), is(0));
    }
    
    private void enableOwnLocation() {
        sharedPreferences.edit().putBoolean(PreferenceKey.SHOW_LOCATION.toString(), true).commit();
        ownLocationOverlay.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.SHOW_LOCATION.toString());
    }

    private void updateLocation()
    {
        ownLocationOverlay.onLocationChanged(mock(Location.class));
    }
}
