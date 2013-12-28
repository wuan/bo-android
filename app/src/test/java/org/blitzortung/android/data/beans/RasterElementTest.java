package org.blitzortung.android.data.beans;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

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
    private RasterParameters rasterParameters;

    private long referenceTimestamp;

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);

        when(rasterParameters.getCenterLongitude(11000)).thenReturn(11.000f);
        when(rasterParameters.getCenterLatitude(49000)).thenReturn(49.000f);

        when(inputArray.getInt(0)).thenReturn(11000);
        when(inputArray.getInt(1)).thenReturn(49000);
        when(inputArray.getInt(2)).thenReturn(3);
        when(inputArray.getInt(3)).thenReturn(5000);

        referenceTimestamp = System.currentTimeMillis();

        rasterElement = new RasterElement(rasterParameters, referenceTimestamp, inputArray);
    }

    @Test
    public void testConstruction() throws JSONException {
        assertThat(rasterElement.getLongitude(), is(11.000f));
        assertThat(rasterElement.getLatitude(), is(49.000f));
        assertThat(rasterElement.getMultiplicity(), is(3));
        assertThat(rasterElement.getTimestamp(), is(referenceTimestamp + 5000 * 1000));

        verify(inputArray, times(4)).getInt(anyInt());
    }

    @Test
    public void testRuntimeExceptionWhenJSONExceptionIsCatched() throws JSONException {
        inputArray = mock(JSONArray.class);
        when(inputArray.getInt(0)).thenThrow(new JSONException("foo"));

        expectedException.expect(RuntimeException.class);
        rasterElement = new RasterElement(rasterParameters, referenceTimestamp, inputArray);
    }
}
