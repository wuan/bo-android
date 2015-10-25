package org.blitzortung.android.alert;

import android.content.res.Resources;

import org.blitzortung.android.alert.handler.AlertStatusHandler;
import org.blitzortung.android.alert.data.AlertStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19)
public class AlertLabelHandlerTest {

    private AlertLabelHandler alertLabelHandler;

    @Mock
    private AlertStatusHandler alertStatusHandler;

    @Mock
    private AlertStatus alertStatus;
    
    @Mock
    private AlertResult alertResult;

    @Mock
    private AlertLabel alertLabel;

    private Resources resources;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        resources = RuntimeEnvironment.application.getResources();

        alertLabelHandler = new AlertLabelHandler(alertLabel, resources);
    }

    @Test
    public void testApplyWithNoAlarm()
    {
        alertLabelHandler.apply(null);

        verify(alertLabel, times(1)).setAlarmTextColor(0xff00ff00);
        verify(alertLabel, times(1)).setAlarmText("");
    }

    @Test
    public void testApplyWithAlarmInHighDistance()
    {
        mockAlarmInRange(50.1f, "SO");

        alertLabelHandler.apply(alertResult);

        verify(alertLabel, times(1)).setAlarmTextColor(0xff00ff00);
        verify(alertLabel, times(1)).setAlarmText("50km SO");
    }

    @Test
    public void testApplyWithAlarmInIntermediateDistance()
    {
        mockAlarmInRange(20.1f, "NW");

        alertLabelHandler.apply(alertResult);

        verify(alertLabel, times(1)).setAlarmTextColor(0xffffff00);
        verify(alertLabel, times(1)).setAlarmText("20km NW");
    }

    @Test
    public void testApplyWithAlarmInMinimumRange()
    {
        mockAlarmInRange(20f, "S");

        alertLabelHandler.apply(alertResult);

        verify(alertLabel, times(1)).setAlarmTextColor(0xffff0000);
        verify(alertLabel, times(1)).setAlarmText("20km S");
    }

    private void mockAlarmInRange(float distance, String sectorLabel)
    {
        when(alertResult.getClosestStrikeDistance()).thenReturn(distance);
        when(alertResult.getDistanceUnitName()).thenReturn("km");
        when(alertResult.getBearingName()).thenReturn(sectorLabel);
    }

}
