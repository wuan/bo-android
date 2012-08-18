package org.blitzortung.android.data.beans;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StrokeTest {

    private Stroke stroke;

    @Mock
    private JSONArray jsonArray;

    long referenceTimestamp = System.currentTimeMillis();

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);

        when(jsonArray.getInt(0)).thenReturn(-10);
        when(jsonArray.getInt(1)).thenReturn(123456789);
        when(jsonArray.getDouble(2)).thenReturn(11.0);
        when(jsonArray.getDouble(3)).thenReturn(49.0);
        when(jsonArray.getDouble(4)).thenReturn(12.3);
        when(jsonArray.getDouble(5)).thenReturn(54.3);
        when(jsonArray.getInt(6)).thenReturn(6);
        when(jsonArray.getInt(7)).thenReturn(1);

        stroke = new Stroke(referenceTimestamp, jsonArray);
    }

    @Test
    public void testConstruct()
    {
        assertThat(stroke.getTimestamp(), is(referenceTimestamp - 10 * 1000 + 123));
        assertThat(stroke.getNanoseconds(), is(456789));
        assertThat(stroke.getLongitude(), is(11.0f));
        assertThat(stroke.getLatitude(), is(49.0f));
        assertThat(stroke.getLateralError(), is(12.3f));
        assertThat(stroke.getAmplitude(), is(54.3f));
        assertThat(stroke.getStationCount(), is((short)6));
        assertThat(stroke.getType(), is((short)1));
    }

    @Test
    public void testConstructFromString()
    {
        stroke = new Stroke("2012-05-12 12:45:23.123456789 49.0 11.0 54.3kA 1 12.3m 6");

        assertThat(stroke.getTimestamp(), is(1336826723123L));
        assertThat(stroke.getNanoseconds(), is(456789));
        assertThat(stroke.getLongitude(), is(11.0f));
        assertThat(stroke.getLatitude(), is(49.0f));
        assertThat(stroke.getLateralError(), is(12.3f));
        assertThat(stroke.getAmplitude(), is(54.3f));
        assertThat(stroke.getStationCount(), is((short)6));
        assertThat(stroke.getType(), is((short)1));
    }
}
