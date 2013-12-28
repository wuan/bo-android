package org.blitzortung.android.data.provider;

import com.google.common.collect.Lists;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.DefaultStroke;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.data.beans.RasterParameters;
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
public class DataResultTest {

    private DataResult dataResult;

    @Before
    public void setUp() {
        dataResult = new DataResult();
    }

    @Test
    public void testConstruction() {
        assertTrue(dataResult.hasFailed());
        assertFalse(dataResult.retrievalWasSuccessful());
        assertFalse(dataResult.containsStrokes());
        assertFalse(dataResult.containsParticipants());
        assertFalse(dataResult.processWasLocked());
        assertFalse(dataResult.containsIncrementalData());
    }

    @Test
    public void testSetStrokes() {
        List<AbstractStroke> strokes = Lists.newArrayList();
        strokes.add(mock(DefaultStroke.class));

        dataResult.setStrokes(strokes);

        assertThat(dataResult.getStrokes(), is(strokes));
        assertFalse(dataResult.hasFailed());
        assertTrue(dataResult.retrievalWasSuccessful());
        assertTrue(dataResult.containsStrokes());
        assertFalse(dataResult.containsParticipants());
        assertFalse(dataResult.processWasLocked());
        assertFalse(dataResult.containsIncrementalData());
    }

    @Test
    public void testSetParticipants() {
        List<Station> stations = Lists.newArrayList();
        stations.add(mock(Station.class));

        dataResult.setParticipants(stations);

        assertThat(dataResult.getParticipants(), is(stations));
        assertTrue(dataResult.hasFailed());
        assertFalse(dataResult.retrievalWasSuccessful());
        assertFalse(dataResult.containsStrokes());
        assertTrue(dataResult.containsParticipants());
        assertFalse(dataResult.processWasLocked());
        assertFalse(dataResult.containsIncrementalData());
    }

    @Test
    public void testSetProcessWasLocked()
    {
        dataResult.setProcessWasLocked();

        assertTrue(dataResult.processWasLocked());
    }

    @Test
    public void testSetGetHasRaster() {
        assertFalse(dataResult.hasRasterParameters());
        assertThat(dataResult.getRasterParameters(), is(nullValue()));

        RasterParameters rasterParameters = mock(RasterParameters.class);
        dataResult.setRasterParameters(rasterParameters);

        assertTrue(dataResult.hasRasterParameters());
        assertThat(dataResult.getRasterParameters(), is(rasterParameters));
    }
}
