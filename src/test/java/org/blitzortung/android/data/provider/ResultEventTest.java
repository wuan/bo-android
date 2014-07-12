package org.blitzortung.android.data.provider;

import com.google.common.collect.Lists;
import org.blitzortung.android.data.beans.*;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.beans.DefaultStrike;
import org.blitzortung.android.data.provider.result.ResultEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ResultEventTest {

    private ResultEvent resultEvent;

    @Before
    public void setUp() {
        resultEvent = new ResultEvent();
    }

    @Test
    public void testConstruction() {
        assertTrue(resultEvent.hasFailed());
        assertFalse(resultEvent.containsStrikes());
        assertFalse(resultEvent.containsParticipants());
        assertFalse(resultEvent.containsIncrementalData());
    }

    @Test
    public void testSetStrikes() {
        List<StrikeAbstract> strikes = Lists.newArrayList();
        strikes.add(mock(DefaultStrike.class));

        resultEvent.setStrikes(strikes);

        assertThat(resultEvent.getStrikes(), is(strikes));
        assertFalse(resultEvent.hasFailed());
        assertTrue(resultEvent.containsStrikes());
        assertFalse(resultEvent.containsParticipants());
        assertFalse(resultEvent.containsIncrementalData());
    }

    @Test
    public void testSetParticipants() {
        List<Station> stations = Lists.newArrayList();
        stations.add(mock(Station.class));

        resultEvent.setStations(stations);

        assertThat(resultEvent.getStations(), is(stations));
        assertTrue(resultEvent.hasFailed());
        assertFalse(resultEvent.containsStrikes());
        assertTrue(resultEvent.containsParticipants());
        assertFalse(resultEvent.containsIncrementalData());
    }

    @Test
    public void testSetGetHasRaster() {
        assertFalse(resultEvent.hasRasterParameters());
        assertThat(resultEvent.getRasterParameters(), is(nullValue()));

        RasterParameters rasterParameters = mock(RasterParameters.class);
        resultEvent.setRasterParameters(rasterParameters);

        assertTrue(resultEvent.hasRasterParameters());
        assertThat(resultEvent.getRasterParameters(), is(rasterParameters));
    }
}
