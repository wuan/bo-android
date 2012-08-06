package org.blitzortung.android.data.beans;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class RasterElementTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private RasterElement rasterElement;

    @Mock
    private JSONArray inputArray;

    @Mock
    private Raster raster;

    long referenceTimestamp;

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);

        when(raster.getCenterLongitude(11000)).thenReturn(11.000f);
        when(raster.getCenterLatitude(49000)).thenReturn(49.000f);

        when(inputArray.getInt(0)).thenReturn(11000);
        when(inputArray.getInt(1)).thenReturn(49000);
        when(inputArray.getInt(2)).thenReturn(3);
        when(inputArray.getInt(3)).thenReturn(5000);

        referenceTimestamp = System.currentTimeMillis();

        rasterElement = new RasterElement(raster, referenceTimestamp, inputArray);
    }

    @Test
    public void testAbstractStrokeConstruction() throws JSONException {
        assertThat(rasterElement.getLongitude(), is(11.000f));
        assertThat(rasterElement.getLatitude(), is(49.000f));
        assertThat(rasterElement.getTimestamp(), is(referenceTimestamp + 5000 * 1000));

        verify(inputArray, times(4)).getInt(anyInt());
    }

    @Test
    public void testRuntimeExceptionWhenJSONExceptionIsCatched() throws JSONException {
        inputArray = mock(JSONArray.class);
        when(inputArray.getInt(0)).thenThrow(new JSONException("foo"));

        expectedException.expect(RuntimeException.class);
        rasterElement = new RasterElement(raster, referenceTimestamp, inputArray);
    }
}
