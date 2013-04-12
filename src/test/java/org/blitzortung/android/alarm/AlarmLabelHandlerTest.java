package org.blitzortung.android.alarm;

import android.content.res.Resources;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.alarm.handler.AlarmStatusHandler;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    private ArgumentCaptor<String> textCaptor;

    private ArgumentCaptor<Integer> colorCaptor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        resources = Robolectric.application.getResources();

        alarmLabelHandler = new AlarmLabelHandler(alarmLabel, resources);

        textCaptor = ArgumentCaptor.forClass(String.class);
        colorCaptor = ArgumentCaptor.forClass(Integer.class);
    }

    @Test
    public void testApplyWithNullValueAsAlarmStatus()
    {
        alarmLabelHandler.apply(null);

        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(textCaptor.getValue(), is(""));
    }

    @Test
    public void testApplyWithNoAlarm()
    {
        alarmLabelHandler.apply(null);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0x0f0));
        assertThat(textCaptor.getValue(), is(""));
    }

    @Test
    public void testApplyWithAlarmInHighDistance()
    {
        mockAlarmInRange(50.1f, "SO");

        alarmLabelHandler.apply(alarmResult);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0x0f0));
        assertThat(textCaptor.getValue(), is("50km SO"));
    }

    @Test
    public void testApplyWithAlarmInIntermediateDistance()
    {
        mockAlarmInRange(20.1f, "NW");

        alarmLabelHandler.apply(alarmResult);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0xff0));
        assertThat(textCaptor.getValue(), is("20km NW"));
    }

    @Test
    public void testApplyWithAlarmInMinimumRange()
    {
        mockAlarmInRange( 20f, "S");

        alarmLabelHandler.apply(alarmResult);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0xf00));
        assertThat(textCaptor.getValue(), is("20km S"));
    }


    private void mockAlarmInRange(float distance, String sectorLabel)
    {
        when(alarmResult.getClosestStrokeDistance()).thenReturn(distance);
        when(alarmResult.getDistanceUnitName()).thenReturn("km");
        when(alarmResult.getBearingName()).thenReturn(sectorLabel);
    }

}
