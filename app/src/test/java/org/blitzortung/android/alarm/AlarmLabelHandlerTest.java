package org.blitzortung.android.alarm;

import android.content.res.Resources;
import org.blitzortung.android.alarm.handler.AlarmStatusHandler;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlarmLabelHandlerTest {

    private AlarmLabelHandler alarmLabelHandler;

    @Mock
    private AlarmStatusHandler alarmStatusHandler;

    @Mock
    private AlarmStatus alarmStatus;
    
    @Mock
    private AlarmResult alarmResult;

    @Mock
    private AlarmLabel alarmLabel;

    private Resources resources;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        resources = Robolectric.application.getResources();

        alarmLabelHandler = new AlarmLabelHandler(alarmLabel, resources);
    }

    @Test
    public void testApplyWithNoAlarm()
    {
        alarmLabelHandler.apply(null);

        verify(alarmLabel, times(1)).setAlarmTextColor(0xff00ff00);
        verify(alarmLabel, times(1)).setAlarmText("");
    }

    @Test
    public void testApplyWithAlarmInHighDistance()
    {
        mockAlarmInRange(50.1f, "SO");

        alarmLabelHandler.apply(alarmResult);

        verify(alarmLabel, times(1)).setAlarmTextColor(0xff00ff00);
        verify(alarmLabel, times(1)).setAlarmText("50km SO");
    }

    @Test
    public void testApplyWithAlarmInIntermediateDistance()
    {
        mockAlarmInRange(20.1f, "NW");

        alarmLabelHandler.apply(alarmResult);

        verify(alarmLabel, times(1)).setAlarmTextColor(0xffffff00);
        verify(alarmLabel, times(1)).setAlarmText("20km NW");
    }

    @Test
    public void testApplyWithAlarmInMinimumRange()
    {
        mockAlarmInRange(20f, "S");

        alarmLabelHandler.apply(alarmResult);

        verify(alarmLabel, times(1)).setAlarmTextColor(0xffff0000);
        verify(alarmLabel, times(1)).setAlarmText("20km S");
    }

    private void mockAlarmInRange(float distance, String sectorLabel)
    {
        when(alarmResult.getClosestStrokeDistance()).thenReturn(distance);
        when(alarmResult.getDistanceUnitName()).thenReturn("km");
        when(alarmResult.getBearingName()).thenReturn(sectorLabel);
    }

}
