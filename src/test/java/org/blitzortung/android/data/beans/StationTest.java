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
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class StationTest {

    private Station station;

    private long currentTime;

    @Before
    public void setUp() {
        currentTime = System.currentTimeMillis();
    }

    @Test
    public void testConstructAndReadValues()
    {
        station = new Station("name", 11.0f, 49.0f, 1336826723123l);

        assertThat(station.getName(), is("name"));
        assertThat(station.getLongitude(), is(11.0f));
        assertThat(station.getLatitude(), is(49.0f));
        assertThat(station.getOfflineSince(), is(1336826723123l));
        assertThat(station.getState(), is(Station.State.OFF));
    }

    @Test
    public void testCreateOnlineStation()
    {
        station = new Station("name", 11.0f, 49.0f, currentTime);

        assertThat(station.getState(), is(Station.State.ON));
    }
}
