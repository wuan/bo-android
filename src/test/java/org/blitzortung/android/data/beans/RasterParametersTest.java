package org.blitzortung.android.data.beans;

import android.graphics.Point;
import android.graphics.RectF;
import com.google.android.maps.Projection;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.Coordsys;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RasterParametersTest {

    private RasterParameters rasterParameters;

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

        rasterParameters = new RasterParameters(jsonObject);
    }

    @Test
    public void testGetCenterLongitude()
    {
        assertThat(rasterParameters.getCenterLongitude(0), is(lon_start + 0.5f * lon_delta));

        assertThat(rasterParameters.getCenterLongitude(lon_count - 1), is(lon_start + (0.5f + lon_count - 1) * lon_delta));
    }

    @Test
    public void testGetCenterLatitude()
    {
        assertThat(rasterParameters.getCenterLatitude(0), is(lat_start - 0.5f * lat_delta));

        assertThat(rasterParameters.getCenterLatitude(lat_count - 1), is(lat_start - (0.5f + lat_count - 1) * lat_delta));
    }

    @Test
    public void testGetLongitudeDelta()
    {
        assertThat(rasterParameters.getLongitudeDelta(), is(lon_delta));
    }

    @Test
    public void testGetLatitudeDelta()
    {
        assertThat(rasterParameters.getLatitudeDelta(), is(lat_delta));
    }

    @Test
    public void testGetRect()
    {
        Projection projection = mock(Projection.class);

        when(projection.toPixels(eq(Coordsys.toMapCoords(lon_start, lat_start)), any(Point.class))).thenReturn(new Point(-5, 5));
        when(projection.toPixels(eq(Coordsys.toMapCoords(lon_start + lon_count * lon_delta, lat_start - lat_count * lat_delta)), any(Point.class))).thenReturn(new Point(5, -5));

        RectF rect = rasterParameters.getRect(projection);

        verify(projection, times(1)).toPixels(eq(Coordsys.toMapCoords(lon_start, lat_start)), any(Point.class));
        verify(projection, times(1)).toPixels(eq(Coordsys.toMapCoords(lon_start + lon_count * lon_delta, lat_start - lat_count * lat_delta)), any(Point.class));

        // TODO RectF is not created correctly through Robolectric
        assertThat(rect.height(), is(0f));
        assertThat(rect.width(), is(0f));
    }

    @Test
    public void testToString()
    {
        assertThat(rasterParameters.toString(), is("RasterParameters(-10.0000, 1.0000; 60.0000, 1.5000)"));
    }

    @Test
    public void testGetLongitudeIndex()
    {
        assertThat(rasterParameters.getLongitudeIndex(lon_start), is(0));
        assertThat(rasterParameters.getLongitudeIndex(lon_start + lon_delta * lon_count), is(lon_count));
    }

    @Test
    public void testGetLatitudeIndex()
    {
        assertThat(rasterParameters.getLatitudeIndex(lat_start), is(0));
        assertThat(rasterParameters.getLatitudeIndex(lat_start - lat_delta * lat_count), is(lat_count));
    }
}
