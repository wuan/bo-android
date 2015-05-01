package org.blitzortung.android.alert.handler;

import android.location.Location;
import com.google.common.collect.Lists;
import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.object.AlertSector;
import org.blitzortung.android.alert.object.AlertSectorRange;
import org.blitzortung.android.data.beans.Strike;
import org.blitzortung.android.util.MeasurementSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AlertSectorHandlerTest {
    
    @Mock
    private Strike strike;
    
    @Mock
    private Location location;

    @Mock
    private Location strikeLocation;

    private long now;

    private long thresholdTime;

    @Mock
    private AlertSector alertSector;
    
    @Mock
    private AlertSectorRange alertSectorRange1;

    @Mock
    private AlertSectorRange alertSectorRange2;
    
    @Mock
    private AlertParameters alertParameters;
    
    private final MeasurementSystem measurementSystem = MeasurementSystem.METRIC;
    
    private AlertSectorHandler alertSectorHandler;
    private long beforeThresholdTime;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        now = System.currentTimeMillis();
        thresholdTime = now - 10 * 60 * 1000;
        beforeThresholdTime = thresholdTime - 1;
        
        alertSectorHandler = new AlertSectorHandler(alertParameters);
        alertSectorHandler.setCheckStrikeParameters(location, thresholdTime);
        
        when(alertSector.getRanges()).thenReturn(Lists.newArrayList(alertSectorRange1, alertSectorRange2));
        when(alertParameters.getMeasurementSystem()).thenReturn(measurementSystem);
        when(alertSectorRange1.getRangeMaximum()).thenReturn(2.5f);
        when(alertSectorRange2.getRangeMaximum()).thenReturn(5f);
    }
    
    @Test
    public void testCheckWithNullAsSector()
    {
        alertSectorHandler.checkStrike(null, strike);
        
        verify(location, times(0)).distanceTo(any(Location.class));
        verify(alertParameters, times(0)).getMeasurementSystem();
    }
    
    @Test
    public void testCheckWithinThresholdTimeAndRange1()
    {
        when(strike.getTimestamp()).thenReturn(thresholdTime);
        when(strike.getLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.distanceTo(strikeLocation)).thenReturn(2500f);
        
        alertSectorHandler.checkStrike(alertSector, strike);
        
        verify(alertSector, times(1)).updateClosestStrikeDistance(2.5f);
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(1)).addStrike(strike);
        verify(alertSectorRange2, times(0)).getRangeMaximum();
    }

    @Test
    public void testCheckWithinThresholdTimeAndOutOfAllRanges()
    {
        when(strike.getTimestamp()).thenReturn(thresholdTime);
        when(strike.getLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.distanceTo(strikeLocation)).thenReturn(5000.1f);

        alertSectorHandler.checkStrike(alertSector, strike);

        verify(alertSector, times(0)).updateClosestStrikeDistance(anyFloat());
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(0)).addStrike(any(Strike.class));
        verify(alertSectorRange2, times(1)).getRangeMaximum();
        verify(alertSectorRange2, times(0)).addStrike(any(Strike.class));
    }

    @Test
    public void testCheckOutOfThresholdTimeAndWithinRange2()
    {
        when(strike.getTimestamp()).thenReturn(beforeThresholdTime);
        when(strike.getLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.distanceTo(strikeLocation)).thenReturn(2500.1f);

        alertSectorHandler.checkStrike(alertSector, strike);

        verify(alertSector, times(0)).updateClosestStrikeDistance(anyFloat());
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(0)).addStrike(any(Strike.class));
        verify(alertSectorRange2, times(1)).getRangeMaximum();
        verify(alertSectorRange2, times(1)).addStrike(strike);
    }

    @Test
    public void testCheckOutOfThresholdTimeAndAllRanges()
    {
        when(strike.getTimestamp()).thenReturn(beforeThresholdTime);
        when(strike.getLocation(any(Location.class))).thenReturn(strikeLocation);
        when(location.distanceTo(strikeLocation)).thenReturn(5000.1f);

        alertSectorHandler.checkStrike(alertSector, strike);

        verify(alertSector, times(0)).updateClosestStrikeDistance(anyFloat());
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(0)).addStrike(any(Strike.class));
        verify(alertSectorRange2, times(1)).getRangeMaximum();
        verify(alertSectorRange2, times(0)).addStrike(any(Strike.class));
    }
}
