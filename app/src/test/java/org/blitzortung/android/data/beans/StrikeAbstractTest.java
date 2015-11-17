package org.blitzortung.android.data.beans;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class StrikeAbstractTest {

    private StrikeAbstractForTest abstractStrike;

    @Before
    public void setUp() {
        abstractStrike = new StrikeAbstractForTest();
    }

    @Test
    public void testGetLongitude() throws Exception {
        assertThat(abstractStrike.getLongitude(), is(11.0f));
    }

    @Test
    public void testGetLatitude() throws Exception {
        assertThat(abstractStrike.getLatitude(), is(49.0f));
    }

    @Test
    public void testGetTimestamp() throws Exception {
        assertThat(abstractStrike.getTimestamp(), is(1234l));
    }

    @Test
    public void testGetMultiplicity() throws Exception {
        assertThat(abstractStrike.getMultiplicity(), is(1));
    }

    @Test
    public void testGetLocation() throws Exception {
        Location location = abstractStrike.getLocation(new Location(""));

        assertThat(location.getLongitude(), is(11.0));
        assertThat(location.getLatitude(), is(49.0));
        assertThat(location.getAltitude(), is(0.0));
    }

    static class StrikeAbstractForTest extends StrikeAbstract {
        public StrikeAbstractForTest() {
            super(1234l, 11.0f, 49.0f);
        }
    }
}
