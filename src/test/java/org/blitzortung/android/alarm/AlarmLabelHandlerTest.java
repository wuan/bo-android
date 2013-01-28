package org.blitzortung.android.alarm;

import android.content.res.Resources;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
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

        assertThat(textCaptor.getValue().toString(), is(""));
    }

    @Test
    public void testApplyWithNoAlarm()
    {
        when(alarmStatus.getSectorWithClosestStroke()).thenReturn(-1);

        alarmLabelHandler.apply(alarmStatus);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0x0f0));
        assertThat(textCaptor.getValue().toString(), is(""));
    }

    @Test
    public void testApplyWithAlarmInHighDistance()
    {
        mockAlarmInRange(50.1f, "SO");

        alarmLabelHandler.apply(alarmStatus);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0x0f0));
        assertThat(textCaptor.getValue().toString(), is("50km SO"));
    }

    @Test
    public void testApplyWithAlarmInIntermediateDistance()
    {
        mockAlarmInRange(20.1f, "NW");

        alarmLabelHandler.apply(alarmStatus);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0xff0));
        assertThat(textCaptor.getValue().toString(), is("20km NW"));
    }

    @Test
    public void testApplyWithAlarmInMinimumRange()
    {
        mockAlarmInRange( 20f, "S");

        alarmLabelHandler.apply(alarmStatus);

        verify(alarmLabel, times(1)).setAlarmTextColor(colorCaptor.capture());
        verify(alarmLabel, times(1)).setAlarmText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0xf00));
        assertThat(textCaptor.getValue().toString(), is("20km S"));
    }


    private void mockAlarmInRange(float distance, String sectorLabel)
    {
        when(alarmStatus.getSectorWithClosestStroke()).thenReturn(0);
        when(alarmStatus.getCurrentActivity()).thenReturn(alarmResult);
        when(alarmResult.getClosestStrokeDistance()).thenReturn(distance);
        when(alarmResult.getDistanceUnitName()).thenReturn("km");
        when(alarmResult.getBearingName()).thenReturn(sectorLabel);
    }

}
