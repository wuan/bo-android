package org.blitzortung.android.alert.data;

import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.factory.AlertObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlertStatusTest {

    @Mock
    private AlertObjectFactory alertObjectFactory;

    @Mock
    private AlertParameters alertParameters;

    @Mock
    private AlertSector alertSector1;

    @Mock
    private AlertSector alertSector2;

    private AlertStatus alertStatus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(alertParameters.getSectorLabels()).thenReturn(new String[]{"foo", "bar"});
        when(alertObjectFactory.createAlarmSector(alertParameters, "foo", 90f, -90f)).thenReturn(alertSector1);
        when(alertObjectFactory.createAlarmSector(alertParameters, "bar", -90f, 90f)).thenReturn(alertSector2);

        alertStatus = new AlertStatus(alertObjectFactory, alertParameters);
    }

    @Test
    public void testConstruct() {
        verify(alertObjectFactory, times(1)).createAlarmSector(alertParameters, "foo", 90f, -90f);
        verify(alertObjectFactory, times(1)).createAlarmSector(alertParameters, "bar", -90f, 90f);
    }

    @Test
    public void testClearResults() {
        alertStatus.clearResults();

        verify(alertSector1, times(1)).clearResults();
        verify(alertSector2, times(1)).clearResults();
    }

    @Test
    public void testGetRanges() {
        final Collection<AlertSector> sectors = alertStatus.getSectors();

        assertThat(sectors).isNotNull();

        assertThat(sectors).hasSize(2);
        assertThat(sectors).contains(alertSector1, alertSector2);
    }

}
