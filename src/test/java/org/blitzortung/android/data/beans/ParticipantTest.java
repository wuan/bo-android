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
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ParticipantTest {

    private Participant participant;

    @Mock
    private JSONArray jsonArray;

    @Before
    public void setUp() throws JSONException {
        MockitoAnnotations.initMocks(this);

        when(jsonArray.getString(1)).thenReturn("<name>");
        when(jsonArray.getDouble(3)).thenReturn(11.0);
        when(jsonArray.getDouble(4)).thenReturn(49.0);

        participant = new Participant(jsonArray);
    }

    @Test
    public void testConstruct()
    {
        assertThat(participant.getName(), is("<name>"));
        assertThat(participant.getLongitude(), is(11.0f));
        assertThat(participant.getLatitude(), is(49.0f));
        assertThat(participant.getState(), is(Participant.State.ON));
    }

    @Test
    public void testConstructWithEmptyOfflineSinceString() throws JSONException {
        when(jsonArray.length()).thenReturn(6);
        when(jsonArray.getString(5)).thenReturn("");

        participant = new Participant(jsonArray);

        assertThat(participant.getName(), is("<name>"));
        assertThat(participant.getLongitude(), is(11.0f));
        assertThat(participant.getLatitude(), is(49.0f));
        assertThat(participant.getState(), is(Participant.State.ON));
    }

    @Test
    public void testConstructWithOfflineSinceString() throws JSONException {
        when(jsonArray.length()).thenReturn(6);
        when(jsonArray.getString(5)).thenReturn("20120512T12:45:23.123");

        participant = new Participant(jsonArray);

        assertThat(participant.getName(), is("<name>"));
        assertThat(participant.getLongitude(), is(11.0f));
        assertThat(participant.getLatitude(), is(49.0f));
        assertThat(participant.getOfflineSince(), is(1336826723123l));
        assertThat(participant.getState(), is(Participant.State.OFF));
    }

    @Test
    public void testConstructFromString()
    {
        participant = new Participant("x x x <name> x 49.0 11.0 2012-05-12&nbsp;12:45:23.123456789");

        assertThat(participant.getName(), is("<name>"));
        assertThat(participant.getLongitude(), is(11.0f));
        assertThat(participant.getLatitude(), is(49.0f));
        assertThat(participant.getOfflineSince(), is(1336826723123l));
        assertThat(participant.getState(), is(Participant.State.OFF));
    }
}
