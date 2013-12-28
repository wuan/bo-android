package org.blitzortung.android.data.beans;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DefaultStrokeTest {

    private DefaultStroke defaultStroke;

    @Test
    public void testConstructAndRead()
    {
        defaultStroke = new DefaultStroke(1336826723123L, 11.0f, 49.0f, 12, 54.3f, (short)6, 12.3f);

        assertThat(defaultStroke.getTimestamp(), is(1336826723123L));
        assertThat(defaultStroke.getLongitude(), is(11.0f));
        assertThat(defaultStroke.getLatitude(), is(49.0f));
        assertThat(defaultStroke.getLateralError(), is(12.3f));
        assertThat(defaultStroke.getAmplitude(), is(54.3f));
        assertThat(defaultStroke.getStationCount(), is((short)6));
    }
}
