package org.blitzortung.android.alarm.object;

import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.factory.AlarmObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collection;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmStatusTest {

    @Mock
    private AlarmObjectFactory alarmObjectFactory;

    @Mock
    private AlarmParameters alarmParameters;

    @Mock
    private AlarmSector alarmSector1;

    @Mock
    private AlarmSector alarmSector2;

    private AlarmStatus alarmStatus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(alarmParameters.getSectorLabels()).thenReturn(new String[]{"foo", "bar"});
        when(alarmObjectFactory.createAlarmSector(alarmParameters, "foo", 90f, -90f)).thenReturn(alarmSector1);
        when(alarmObjectFactory.createAlarmSector(alarmParameters, "bar", -90f, 90f)).thenReturn(alarmSector2);

        alarmStatus = new AlarmStatus(alarmObjectFactory, alarmParameters);
    }
    
    @Test
    public void testConstruct() {
        verify(alarmObjectFactory, times(1)).createAlarmSector(alarmParameters, "foo", 90f, -90f);
        verify(alarmObjectFactory, times(1)).createAlarmSector(alarmParameters, "bar", -90f, 90f);
    }

    @Test
    public void testClearResults() {
        alarmStatus.clearResults();

        verify(alarmSector1, times(1)).clearResults();
        verify(alarmSector2, times(1)).clearResults();
    }

    @Test
    public void testGetRanges() {
        final Collection<AlarmSector> sectors = alarmStatus.getSectors();

        assertThat(sectors, is(not(nullValue())));
        assertThat(sectors, hasSize(2));
        assertThat(sectors, contains(alarmSector1, alarmSector2));
    }

}
