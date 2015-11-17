package org.blitzortung.android.data.beans;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DefaultStrikeTest {

    private DefaultStrike defaultStrike;

    @Test
    public void testConstructAndRead() {
        defaultStrike = new DefaultStrike(1336826723123L, 11.0f, 49.0f, 12, 54.3f, (short) 6, 12.3f);

        assertThat(defaultStrike.getTimestamp(), is(1336826723123L));
        assertThat(defaultStrike.getLongitude(), is(11.0f));
        assertThat(defaultStrike.getLatitude(), is(49.0f));
        assertThat(defaultStrike.getLateralError(), is(12.3f));
        assertThat(defaultStrike.getAmplitude(), is(54.3f));
        assertThat(defaultStrike.getStationCount(), is((short) 6));
    }
}
