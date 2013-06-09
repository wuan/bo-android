package org.blitzortung.android.map.overlay;

import android.location.Location;
import com.google.android.maps.GeoPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class OwnLocationOverlayItemTest {

    private OwnLocationOverlayItem ownLocationOverlayItem;

    @Mock
    private Location location;

    private float radius;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(location.getLongitude()).thenReturn(11.0);
        when(location.getLatitude()).thenReturn(49.0);

        radius = 2.5f;

        ownLocationOverlayItem = new OwnLocationOverlayItem(location, radius);
    }

    @Test
    public void testConstruction()
    {

        GeoPoint point = ownLocationOverlayItem.getPoint();

        assertThat(point.getLongitudeE6(), is(11000000));
        assertThat(point.getLatitudeE6(), is(49000000));
    }

    @Test
    public void testGetRadius()
    {
        assertThat(ownLocationOverlayItem.getRadius(), is(2.5f));
    }



}
