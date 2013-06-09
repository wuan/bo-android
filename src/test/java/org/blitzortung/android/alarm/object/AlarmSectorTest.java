package org.blitzortung.android.alarm.object;

import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmSectorTest {

    @Mock
    private AlarmObjectFactory alarmObjectFactory;

    @Mock
    private AlarmParameters alarmParameters;

    private final String sectorLabel = "foo";

    @Mock
    private AlarmSectorRange alarmSectorRange1;

    @Mock
    private AlarmSectorRange alarmSectorRange2;

    private float minimumBearing = 10f;

    private float maximumBearing = 20f;

    private AlarmSector alarmSector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(alarmParameters.getSectorLabels()).thenReturn(new String[]{"eins", "zwei"});
        when(alarmParameters.getRangeSteps()).thenReturn(new float[]{10f, 20f});
        when(alarmObjectFactory.createAlarmSectorRange(0.0f, 10.0f)).thenReturn(alarmSectorRange1);
        when(alarmObjectFactory.createAlarmSectorRange(10.0f, 20.0f)).thenReturn(alarmSectorRange2);

        alarmSector = new AlarmSector(alarmObjectFactory, alarmParameters, sectorLabel, minimumBearing, maximumBearing);
    }

    @Test
    public void testClearResults() {
        alarmSector.updateClosestStrokeDistance(10.0f);
        
        alarmSector.clearResults();

        assertThat(alarmSector.getClosestStrokeDistance(),is (Float.POSITIVE_INFINITY));
        verify(alarmSectorRange1, times(1)).clearResults();
        verify(alarmSectorRange2, times(1)).clearResults();
    }

    @Test
    public void testGetRanges() {
        final List<AlarmSectorRange> ranges = alarmSector.getRanges();

        assertThat(ranges, is(not(nullValue())));
        assertThat(ranges, hasSize(2));
        assertThat(ranges, contains(alarmSectorRange1, alarmSectorRange2));
    }

    @Test
    public void testGetMinimumSectorBearing() {
        assertThat(alarmSector.getMinimumSectorBearing(), is(minimumBearing));
    }

    @Test
    public void testGetMaximumSectorBearing() {
        assertThat(alarmSector.getMaximumSectorBearing(), is(maximumBearing));
    }
    
    @Test
    public void testGetLabel() {
        assertThat(alarmSector.getLabel(), is(sectorLabel));
    }
    
    @Test
    public void testGetClosestStrokeDistanceAndUpdateClosestStrokeDistance()
    {
        assertThat(alarmSector.getClosestStrokeDistance(), is(Float.POSITIVE_INFINITY));
        
        alarmSector.updateClosestStrokeDistance(25.0f);

        assertThat(alarmSector.getClosestStrokeDistance(), is(25f));

        alarmSector.updateClosestStrokeDistance(10.0f);

        assertThat(alarmSector.getClosestStrokeDistance(), is(10f));

        alarmSector.updateClosestStrokeDistance(25.0f);

        assertThat(alarmSector.getClosestStrokeDistance(), is(10f));
    }

}
