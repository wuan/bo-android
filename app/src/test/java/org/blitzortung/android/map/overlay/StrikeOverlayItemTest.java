package org.blitzortung.android.map.overlay;

import com.google.android.maps.GeoPoint;
import org.blitzortung.android.data.beans.StrikeAbstract;
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
public class StrikeOverlayItemTest {

    private StrikeOverlayItem strikeOverlayItem;

    @Mock
    private StrikeAbstract strike;

    private long time;
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        time = System.currentTimeMillis();

        when(strike.getLongitude()).thenReturn(11.0f);
        when(strike.getLatitude()).thenReturn(49.0f);
        when(strike.getTimestamp()).thenReturn(time);
        when(strike.getMultiplicity()).thenReturn(3);

        strikeOverlayItem = new StrikeOverlayItem(strike);
    }

    @Test
    public void testConstruction()
    {
        GeoPoint point = strikeOverlayItem.getPoint();

        assertThat(point.getLongitudeE6(), is(11000000));
        assertThat(point.getLatitudeE6(), is(49000000));
    }

    @Test
    public void testGetTimestamp()
    {
        assertThat(strikeOverlayItem.getTimestamp(), is(time));
    }

    @Test
    public void testGetMultiplicity()
    {
        assertThat(strikeOverlayItem.getMultiplicity(), is(3));
    }



}
