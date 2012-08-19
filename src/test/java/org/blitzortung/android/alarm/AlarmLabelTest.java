package org.blitzortung.android.alarm;

import android.content.res.Resources;
import android.widget.TextView;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.app.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlarmLabelTest {

    private AlarmLabel alarmLabel;

    @Mock
    private AlarmStatus alarmStatus;

    @Mock
    private AlarmResult alarmResult;

    @Mock
    private TextView textView;

    private Resources resources;

    private ArgumentCaptor<CharSequence> textCaptor;

    private ArgumentCaptor<Integer> colorCaptor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        resources = Robolectric.application.getResources();

        alarmLabel = new AlarmLabel(textView, resources);

        textCaptor = ArgumentCaptor.forClass(CharSequence.class);
        colorCaptor = ArgumentCaptor.forClass(Integer.class);
    }

    @Test
    public void testApplyWithNullValueAsAlarmStatus()
    {
        alarmLabel.apply(null);

        verify(textView, times(1)).setText(textCaptor.capture());

        assertThat(textCaptor.getValue().toString(), is(""));
    }

    @Test
    public void testApplyWithNoAlarm()
    {
        when(alarmStatus.getSectorWithClosestStroke()).thenReturn(-1);

        alarmLabel.apply(alarmStatus);

        verify(textView, times(1)).setTextColor(colorCaptor.capture());
        verify(textView, times(1)).setText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0x0f0));
        assertThat(textCaptor.getValue().toString(), is("*"));
    }

    @Test
    public void testApplyWithAlarmInRangeHigherThanThree()
    {
        mockAlarmInRange(4, 260000f, "SO");

        alarmLabel.apply(alarmStatus);

        verify(textView, times(1)).setTextColor(colorCaptor.capture());
        verify(textView, times(1)).setText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0x0f0));
        assertThat(textCaptor.getValue().toString(), is("260km SO"));
    }

    @Test
    public void testApplyWithAlarmInRangeHigherThanOne()
    {
        mockAlarmInRange(2, 100000f, "NW");

        alarmLabel.apply(alarmStatus);

        verify(textView, times(1)).setTextColor(colorCaptor.capture());
        verify(textView, times(1)).setText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0xff0));
        assertThat(textCaptor.getValue().toString(), is("100km NW"));
    }

    @Test
    public void testApplyWithAlarmInMinimumRange()
    {
        mockAlarmInRange(0, 15000f, "S");

        alarmLabel.apply(alarmStatus);

        verify(textView, times(1)).setTextColor(colorCaptor.capture());
        verify(textView, times(1)).setText(textCaptor.capture());

        assertThat(colorCaptor.getValue(), is(0xf00));
        assertThat(textCaptor.getValue().toString(), is("15km S"));
    }


    private void mockAlarmInRange(int range, float distance, String sectorLabel)
    {
        when(alarmStatus.getSectorWithClosestStroke()).thenReturn(0);
        when(alarmStatus.currentActivity()).thenReturn(alarmResult);
        when(alarmStatus.getSectorLabel(2)).thenReturn(sectorLabel);
        when(alarmResult.getRange()).thenReturn(range);
        when(alarmResult.getDistance()).thenReturn(distance);
        when(alarmResult.getSector()).thenReturn(2);
    }

}
