package org.blitzortung.android.data.beans;

import android.location.Location;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class AbstractStrokeTest {

    static class AbstractStrokeForTest extends AbstractStroke
    {
        public AbstractStrokeForTest()
        {
            setTimestamp(1234l);
            setLongitude(11.0f);
            setLatitude(49.0f);
        }
    }

    private AbstractStrokeForTest abstractStroke;

    @Before
    public void setUp()
    {
         abstractStroke = new AbstractStrokeForTest();
    }

    @Test
    public void testGetLongitude() throws Exception {
        assertThat(abstractStroke.getLongitude(), is(11.0f));
    }

    @Test
    public void testGetLatitude() throws Exception {
        assertThat(abstractStroke.getLatitude(), is(49.0f));
    }

    @Test
    public void testGetTimestamp() throws Exception {
        assertThat(abstractStroke.getTimestamp(), is(1234l));
    }

    @Test
    public void testGetMultiplicity() throws Exception {
        assertThat(abstractStroke.getMultiplicity(), is(1));
    }

    @Test
    public void testGetLocation() throws Exception {
        Location location = abstractStroke.getLocation(new Location(""));

        assertThat(location.getLongitude(), is(11.0));
        assertThat(location.getLatitude(), is(49.0));
        assertThat(location.getAltitude(), is(0.0));
    }
}
