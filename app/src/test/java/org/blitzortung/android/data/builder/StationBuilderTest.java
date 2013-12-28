package org.blitzortung.android.data.builder;

import org.blitzortung.android.data.beans.Station;
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StationBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private JSONArray jsonArray;

    private StationBuilder builder;

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);

        when(jsonArray.getString(1)).thenReturn("name");
        when(jsonArray.getDouble(3)).thenReturn(11.0);
        when(jsonArray.getDouble(4)).thenReturn(49.0);

        builder = new StationBuilder();
    }

    @Test
    public void testBuildFromJson() throws JSONException {

        Station station = builder.fromJson(jsonArray);

        assertThat(station.getName(), is("name"));
        assertThat(station.getLongitude(), is(11.0f));
        assertThat(station.getLatitude(), is(49.0f));
        assertThat(station.getState(), is(Station.State.ON));
    }

    @Test
    public void testBuildFromJsonWithEmptyOfflineSinceString() throws JSONException {
        when(jsonArray.length()).thenReturn(6);
        when(jsonArray.getString(5)).thenReturn("");

        Station station = builder.fromJson(jsonArray);

        assertThat(station.getName(), is("name"));
        assertThat(station.getLongitude(), is(11.0f));
        assertThat(station.getLatitude(), is(49.0f));
        assertThat(station.getState(), is(Station.State.ON));
    }

    @Test
    public void testBuildFromJsonWithOfflineSinceStringSet() throws JSONException {
        when(jsonArray.length()).thenReturn(6);
        when(jsonArray.getString(5)).thenReturn("20120512T12:45:23.123");

        Station station = builder.fromJson(jsonArray);

        assertThat(station.getName(), is("name"));
        assertThat(station.getLongitude(), is(11.0f));
        assertThat(station.getLatitude(), is(49.0f));
        assertThat(station.getState(), is(Station.State.OFF));
    }

    @Test
    public void testExceptionHandlingDuringConstruction() throws JSONException {
        when(jsonArray.getString(1)).thenThrow(new JSONException("foo"));

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("error with JSON format while parsing participants data");
        builder.fromJson(jsonArray);
    }

}
