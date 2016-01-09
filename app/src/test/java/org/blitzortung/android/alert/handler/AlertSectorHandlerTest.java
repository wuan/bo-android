package org.blitzortung.android.alert.handler;

import android.location.Location;
import com.google.common.collect.Lists;
import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.object.AlertSector;
import org.blitzortung.android.alert.object.AlertSectorRange;
import org.blitzortung.android.data.beans.Stroke;
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
    private Stroke stroke;
    
    @Mock
    private Location location;

    @Mock
    private Location strokeLocation;

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
        alertSectorHandler.setCheckStrokeParameters(location, thresholdTime);
        
        when(alertSector.getRanges()).thenReturn(Lists.newArrayList(alertSectorRange1, alertSectorRange2));
        when(alertParameters.getMeasurementSystem()).thenReturn(measurementSystem);
        when(alertSectorRange1.getRangeMaximum()).thenReturn(2.5f);
        when(alertSectorRange2.getRangeMaximum()).thenReturn(5f);
    }
    
    @Test
    public void testCheckWithNullAsSector()
    {
        alertSectorHandler.checkStroke(null, stroke);
        
        verify(location, times(0)).distanceTo(any(Location.class));
        verify(alertParameters, times(0)).getMeasurementSystem();
    }
    
    @Test
    public void testCheckWithinThresholdTimeAndRange1()
    {
        when(stroke.getTimestamp()).thenReturn(thresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(2500f);
        
        alertSectorHandler.checkStroke(alertSector, stroke);
        
        verify(alertSector, times(1)).updateClosestStrokeDistance(2.5f);
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(1)).addStroke(stroke);
        verify(alertSectorRange2, times(0)).getRangeMaximum();
    }

    @Test
    public void testCheckWithinThresholdTimeAndOutOfAllRanges()
    {
        when(stroke.getTimestamp()).thenReturn(thresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(5000.1f);

        alertSectorHandler.checkStroke(alertSector, stroke);

        verify(alertSector, times(0)).updateClosestStrokeDistance(anyFloat());
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(0)).addStroke(any(Stroke.class));
        verify(alertSectorRange2, times(1)).getRangeMaximum();
        verify(alertSectorRange2, times(0)).addStroke(any(Stroke.class));
    }

    @Test
    public void testCheckOutOfThresholdTimeAndWithinRange2()
    {
        when(stroke.getTimestamp()).thenReturn(beforeThresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(2500.1f);

        alertSectorHandler.checkStroke(alertSector, stroke);

        verify(alertSector, times(0)).updateClosestStrokeDistance(anyFloat());
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(0)).addStroke(any(Stroke.class));
        verify(alertSectorRange2, times(1)).getRangeMaximum();
        verify(alertSectorRange2, times(1)).addStroke(stroke);
    }

    @Test
    public void testCheckOutOfThresholdTimeAndAllRanges()
    {
        when(stroke.getTimestamp()).thenReturn(beforeThresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(5000.1f);

        alertSectorHandler.checkStroke(alertSector, stroke);

        verify(alertSector, times(0)).updateClosestStrokeDistance(anyFloat());
        verify(alertSectorRange1, times(1)).getRangeMaximum();
        verify(alertSectorRange1, times(0)).addStroke(any(Stroke.class));
        verify(alertSectorRange2, times(1)).getRangeMaximum();
        verify(alertSectorRange2, times(0)).addStroke(any(Stroke.class));
    }
}
