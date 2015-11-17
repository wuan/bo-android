package org.blitzortung.android.data.provider;

import com.google.common.collect.Lists;

import org.blitzortung.android.data.beans.DefaultStrike;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.provider.result.ResultEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class ResultEventTest {

    @Test
    public void testConstruction() {
        final ResultEvent resultEvent = ResultEvent.builder().build();
        assertTrue(resultEvent.hasFailed());
        assertFalse(resultEvent.containsStrikes());
        assertFalse(resultEvent.containsParticipants());
        assertFalse(resultEvent.isIncrementalData());
    }

    @Test
    public void testSetStrikes() {
        List<StrikeAbstract> strikes = Lists.newArrayList();
        strikes.add(mock(DefaultStrike.class));

        final ResultEvent resultEvent = ResultEvent.builder().strikes(strikes).build();

        assertThat(resultEvent.getStrikes(), is(strikes));
        assertFalse(resultEvent.hasFailed());
        assertTrue(resultEvent.containsStrikes());
        assertFalse(resultEvent.containsParticipants());
        assertFalse(resultEvent.isIncrementalData());
    }

    @Test
    public void testSetParticipants() {
        List<Station> stations = Lists.newArrayList();
        stations.add(mock(Station.class));

        final ResultEvent resultEvent = ResultEvent.builder().stations(stations).build();

        assertThat(resultEvent.getStations(), is(stations));
        assertTrue(resultEvent.hasFailed());
        assertFalse(resultEvent.containsStrikes());
        assertTrue(resultEvent.containsParticipants());
        assertFalse(resultEvent.isIncrementalData());
    }

    @Test
    public void testSetGetHasRaster() {
        ResultEvent resultEvent = ResultEvent.builder().build();
        assertFalse(resultEvent.hasRasterParameters());
        assertThat(resultEvent.getRasterParameters(), is(nullValue()));

        RasterParameters rasterParameters = mock(RasterParameters.class);
        resultEvent = ResultEvent.builder().rasterParameters(rasterParameters).build();

        assertTrue(resultEvent.hasRasterParameters());
        assertThat(resultEvent.getRasterParameters(), is(rasterParameters));
    }
}
