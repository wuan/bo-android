package org.blitzortung.android.alert.data;

import org.blitzortung.android.data.beans.Strike;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlertSectorRangeTest {

    private AlertSectorRange alertSectorRange;

    @Before
    public void setUp() {
        alertSectorRange = new AlertSectorRange(5.0f, 10.0f);
    }

    @Test
    public void testGetRangeMinimum() {
        assertThat(alertSectorRange.getRangeMinimum(), is(5.0f));
    }

    @Test
    public void testGetRangeMaximum() {
        assertThat(alertSectorRange.getRangeMaximum(), is(10.0f));
    }

    @Test
    public void testGetStrikeCountInitialValue() {
        assertThat(alertSectorRange.getStrikeCount(), is(0));
    }

    @Test
    public void testSetGetStrikeCount() {
        Strike strike = mock(Strike.class);
        when(strike.getMultiplicity()).thenReturn(1).thenReturn(2);

        alertSectorRange.addStrike(strike);
        assertThat(alertSectorRange.getStrikeCount(), is(1));

        alertSectorRange.addStrike(strike);
        assertThat(alertSectorRange.getStrikeCount(), is(3));
    }

    @Test
    public void testGetLatestStrikeTimestampInitialValue() {
        assertThat(alertSectorRange.getLatestStrikeTimestamp(), is(0l));
    }

    @Test
    public void testGetLatestStrikeTimestamp() {
        Strike strike = mock(Strike.class);
        when(strike.getTimestamp()).thenReturn(1000l).thenReturn(5000l);

        alertSectorRange.addStrike(strike);
        assertThat(alertSectorRange.getLatestStrikeTimestamp(), is(1000l));

        alertSectorRange.addStrike(strike);
        assertThat(alertSectorRange.getLatestStrikeTimestamp(), is(5000l));
    }

    @Test
    public void testReset() {
        Strike strike = mock(Strike.class);
        when(strike.getTimestamp()).thenReturn(5000l);
        when(strike.getMultiplicity()).thenReturn(2);

        alertSectorRange.clearResults();

        assertThat(alertSectorRange.getStrikeCount(), is(0));
        assertThat(alertSectorRange.getLatestStrikeTimestamp(), is(0l));
    }
}
