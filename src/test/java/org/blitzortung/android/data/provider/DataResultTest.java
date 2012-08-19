package org.blitzortung.android.data.provider;

import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Participant;
import org.blitzortung.android.data.beans.Raster;
import org.blitzortung.android.data.beans.Stroke;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isNotNull;
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
        assertFalse(dataResult.isIncremental());
    }

    @Test
    public void testSetStrokes() {
        List<AbstractStroke> strokes = Lists.newArrayList();
        strokes.add(mock(Stroke.class));

        dataResult.setStrokes(strokes);

        assertThat(dataResult.getStrokes(), is(strokes));
        assertFalse(dataResult.hasFailed());
        assertTrue(dataResult.retrievalWasSuccessful());
        assertTrue(dataResult.containsStrokes());
        assertFalse(dataResult.containsParticipants());
        assertFalse(dataResult.processWasLocked());
        assertFalse(dataResult.isIncremental());
    }

    @Test
    public void testSetParticipants() {
        List<Participant> participants = Lists.newArrayList();
        participants.add(mock(Participant.class));

        dataResult.setParticipants(participants);

        assertThat(dataResult.getParticipants(), is(participants));
        assertTrue(dataResult.hasFailed());
        assertFalse(dataResult.retrievalWasSuccessful());
        assertFalse(dataResult.containsStrokes());
        assertTrue(dataResult.containsParticipants());
        assertFalse(dataResult.processWasLocked());
        assertFalse(dataResult.isIncremental());
    }

    @Test
    public void testSetProcessWasLocked()
    {
        dataResult.setProcessWasLocked();

        assertTrue(dataResult.processWasLocked());
    }

    @Test
    public void testSetGetHasRaster() {
        assertFalse(dataResult.hasRaster());
        assertThat(dataResult.getRaster(), is(nullValue()));

        Raster raster = mock(Raster.class);
        dataResult.setRaster(raster);

        assertTrue(dataResult.hasRaster());
        assertThat(dataResult.getRaster(), is(raster));
    }
}
