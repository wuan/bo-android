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
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DefaultStrokeTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private DefaultStroke defaultStroke;

    @Mock
    private JSONArray jsonArray;

    private final long referenceTimestamp = System.currentTimeMillis();

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);

        when(jsonArray.getInt(0)).thenReturn(10);
        when(jsonArray.getDouble(1)).thenReturn(11.0);
        when(jsonArray.getDouble(2)).thenReturn(49.0);
        when(jsonArray.getDouble(3)).thenReturn(12.3);
        when(jsonArray.getDouble(4)).thenReturn(54.3);
        when(jsonArray.getInt(5)).thenReturn(6);
        when(jsonArray.getInt(6)).thenReturn(1);

        defaultStroke = new DefaultStroke(referenceTimestamp, jsonArray);
    }

    @Test
    public void testConstruct()
    {
        assertThat(defaultStroke.getTimestamp(), is(referenceTimestamp - 10 * 1000));
        assertThat(defaultStroke.getLongitude(), is(11.0f));
        assertThat(defaultStroke.getLatitude(), is(49.0f));
        assertThat(defaultStroke.getLateralError(), is(12.3f));
        assertThat(defaultStroke.getAmplitude(), is(54.3f));
        assertThat(defaultStroke.getStationCount(), is((short)6));
        assertThat(defaultStroke.getType(), is((short)1));
    }

    @Test
    public void testExceptionHandlingDuringConstruction() throws JSONException {
        when(jsonArray.getInt(0)).thenThrow(JSONException.class);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("error with JSON format while parsing stroke data");
        defaultStroke = new DefaultStroke(referenceTimestamp, jsonArray);
    }

    @Test
    public void testConstructFromString()
    {
        String line =  "2012-05-12 12:45:23.123456789 49.0 11.0 54.3kA 1 12.3m 6";
        String[] fields = line.split(" ");
        defaultStroke = new DefaultStroke(1336826723123L, fields);

        assertThat(defaultStroke.getTimestamp(), is(1336826723123L));
        assertThat(defaultStroke.getLongitude(), is(11.0f));
        assertThat(defaultStroke.getLatitude(), is(49.0f));
        assertThat(defaultStroke.getLateralError(), is(12.3f));
        assertThat(defaultStroke.getAmplitude(), is(54.3f));
        assertThat(defaultStroke.getStationCount(), is((short)6));
        assertThat(defaultStroke.getType(), is((short)1));
    }
}
