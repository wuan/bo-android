package org.blitzortung.android.map.overlay;

import android.location.Location;
import com.google.android.maps.GeoPoint;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StrokeOverlayItemTest {

    private StrokeOverlayItem strokeOverlayItem;

    @Mock
    private AbstractStroke stroke;

    private long time;
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        time = System.currentTimeMillis();

        when(stroke.getLongitude()).thenReturn(11.0f);
        when(stroke.getLatitude()).thenReturn(49.0f);
        when(stroke.getTimestamp()).thenReturn(time);
        when(stroke.getMultiplicity()).thenReturn(3);

        strokeOverlayItem = new StrokeOverlayItem(stroke);
    }

    @Test
    public void testConstruction()
    {
        GeoPoint point = strokeOverlayItem.getPoint();

        assertThat(point.getLongitudeE6(), is(11000000));
        assertThat(point.getLatitudeE6(), is(49000000));
    }

    @Test
    public void testGetTimestamp()
    {
        assertThat(strokeOverlayItem.getTimestamp(), is(time));
    }

    @Test
    public void testGetMultiplicity()
    {
        assertThat(strokeOverlayItem.getMultiplicity(), is(3));
    }



}
