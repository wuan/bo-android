package org.blitzortung.android.data.beans;

import android.graphics.Point;
import android.graphics.RectF;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class RasterTest {

    private Raster raster;

    @Mock
    private JSONObject jsonObject;

    float lon_start = -10;
    float lon_delta = 1;
    int lon_count = 40;
    float lat_start = 60;
    float lat_delta = 1.5f;
    int lat_count = 30;

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);

        when(jsonObject.getDouble("x0")).thenReturn((double)lon_start);
        when(jsonObject.getDouble("y1")).thenReturn((double)lat_start);
        when(jsonObject.getDouble("xd")).thenReturn((double)lon_delta);
        when(jsonObject.getDouble("yd")).thenReturn((double)lat_delta);
        when(jsonObject.getInt("xc")).thenReturn(lon_count);
        when(jsonObject.getInt("yc")).thenReturn(lat_count);

        raster = new Raster(jsonObject);
    }

    @Test
    public void testGetCenterLongitude()
    {
        assertThat(raster.getCenterLongitude(0), is(lon_start + 0.5f * lon_delta));

        assertThat(raster.getCenterLongitude(lon_count - 1), is(lon_start + (0.5f + lon_count - 1) * lon_delta));
    }

    @Test
    public void testGetCenterLatitude()
    {
        assertThat(raster.getCenterLatitude(0), is(lat_start - 0.5f * lat_delta));

        assertThat(raster.getCenterLatitude(lat_count - 1), is(lat_start - (0.5f + lat_count - 1) * lat_delta));
    }

    @Test
    public void testGetLongitudeDelta()
    {
        assertThat(raster.getLongitudeDelta(), is(lon_delta));
    }

    @Test
    public void testGetLatitudeDelta()
    {
        assertThat(raster.getLatitudeDelta(), is(lat_delta));
    }

//    @Test
//    public void testGetRect()
//    {
//        Projection projection = mock(Projection.class);
//
//        when(projection.toPixels(new GeoPoint((int)lon_start * 1e6, (int)lat_start * 1000000)))
//        RectF rect = raster.getRect(projection);
//
//        assertThat(rect.height(), is(20f));
//        assertThat(rect.width(), is(30f));
//    }

}
