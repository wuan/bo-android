package org.blitzortung.android.map.overlay;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.common.collect.Lists;

import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.location.LocationEvent;
import org.blitzortung.android.location.LocationHandler;
import org.blitzortung.android.map.OwnMapView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowPreferenceManager;
import org.robolectric.util.Strings;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class OwnLocationOverlayTest {

    @SuppressWarnings({"UnusedDeclaration"})
    @Implements(OverlayItem.class)
    public class ShadowOverlayItem {
        private GeoPoint geoPoint;
        private String title;
        private String snippet;

        public void __constructor__(GeoPoint geoPoint, String title, String snippet) {
            this.geoPoint = geoPoint;
            this.title = title;
            this.snippet = snippet;
        }

        @Implementation
        public GeoPoint getPoint() {
            return geoPoint;
        }

        @Implementation
        public String getTitle() {
            return title;
        }

        @Implementation
        public String getSnippet() {
            return snippet;
        }

        @Override @Implementation
        public boolean equals(Object o) {
            if (o == null) return false;
            //o = shadowOf(o);
            //if (o == null) return false;
            if (this == o) return true;
            if (getClass() != o.getClass()) return false;

            ShadowOverlayItem that = (ShadowOverlayItem) o;

            return Strings.equals(title, that.title)
                    && Strings.equals(snippet, that.snippet)
                    && geoPoint == null ? that.geoPoint == null :
                    geoPoint.equals(that.geoPoint);
        }

        @Override @Implementation
        public int hashCode() {
            int result = 13;
            result = title == null ? result : 19 * result + title.hashCode();
            result = snippet == null ? result : 19 * result + snippet.hashCode();
            result = geoPoint == null ? result : 19 * result + geoPoint.hashCode();
            return result;
        }
    }


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

        sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);

        when(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationHandler);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(context.getResources()).thenReturn(resources);
        when(context.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
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

    @Test
    public void testOnLocationChanged()
    {
        ownLocationOverlay.getLocationEventConsumer().consume(new LocationEvent(mock(Location.class)));
        
        assertThat(ownLocationOverlay.size(), is(1));
    }
    
    @Test
    public void testOnLocationChangedWithNullLocation()
    {
        ownLocationOverlay.getLocationEventConsumer().consume(new LocationEvent(null));

        assertThat(ownLocationOverlay.size(), is(0));
    }

    @Test
    public void testDisableOwnLocation()
    {
        ownLocationOverlay.getLocationEventConsumer().consume(new LocationEvent(mock(Location.class)));

        ownLocationOverlay.disableOwnLocation();

        assertThat(ownLocationOverlay.size(), is(0));
    }
    
    private void enableOwnLocation() {
        sharedPreferences.edit().putBoolean(PreferenceKey.SHOW_LOCATION.toString(), true).commit();
        ownLocationOverlay.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.SHOW_LOCATION.toString());
    }

    private void updateLocation()
    {
        ownLocationOverlay.getLocationEventConsumer().consume(new LocationEvent(mock(Location.class)));
    }
}
