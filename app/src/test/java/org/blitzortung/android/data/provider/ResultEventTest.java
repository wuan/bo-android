package org.blitzortung.android.data.provider;

import com.google.common.collect.Lists;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.DefaultStroke;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.RasterParameters;
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
        assertFalse(resultEvent.containsStrokes());
        assertFalse(resultEvent.containsParticipants());
        assertFalse(resultEvent.containsIncrementalData());
    }

    @Test
    public void testSetStrokes() {
        List<AbstractStroke> strokes = Lists.newArrayList();
        strokes.add(mock(DefaultStroke.class));

        resultEvent.setStrokes(strokes);

        assertThat(resultEvent.getStrokes(), is(strokes));
        assertFalse(resultEvent.hasFailed());
        assertTrue(resultEvent.containsStrokes());
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
        assertFalse(resultEvent.containsStrokes());
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
