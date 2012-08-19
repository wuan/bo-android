package org.blitzortung.android.map.overlay;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import com.google.android.maps.Overlay;
import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import org.blitzortung.android.app.Preferences;
import org.blitzortung.android.map.OwnMapView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static com.xtremelabs.robolectric.Robolectric.bindShadowClass;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class OwnLocationOverlayTest {


    @Implements(PreferenceManager.class)
    static class ShadowPreferenceManager {

        @Implementation
        public static SharedPreferences getDefaultSharedPreferences(Context context) {
            return context.getSharedPreferences("", 0);
        }

    }
    private OwnLocationOverlay ownLocationOverlay;

    @Mock
    Context context;

    @Mock
    OwnMapView mapView;

    @Mock
    LocationManager locationManager;

    @Mock
    SharedPreferences sharedPreferences;

    List<Overlay> overlays = Lists.newArrayList();

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        bindShadowClass(ShadowPreferenceManager.class);

        when(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(mapView.getOverlays()).thenReturn(overlays);

        ownLocationOverlay = new OwnLocationOverlay(context, mapView);
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

    private void enableOwnLocation() {
        when(sharedPreferences.getBoolean(Preferences.SHOW_LOCATION_KEY, false)).thenReturn(true);
        ownLocationOverlay.onSharedPreferenceChanged(sharedPreferences, Preferences.SHOW_LOCATION_KEY);
    }

    private void updateLocation()
    {
        ownLocationOverlay.onLocationChanged(mock(Location.class));
    }
}
