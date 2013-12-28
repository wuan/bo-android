package org.blitzortung.android.alarm.handler;

import android.location.Location;
import com.google.common.collect.Lists;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmSectorRange;
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
public class AlarmSectorHandlerTest {
    
    @Mock
    private Stroke stroke;
    
    @Mock
    private Location location;

    @Mock
    private Location strokeLocation;

    private long now;

    private long thresholdTime;

    @Mock
    private AlarmSector alarmSector;
    
    @Mock
    private AlarmSectorRange alarmSectorRange1;

    @Mock
    private AlarmSectorRange alarmSectorRange2;
    
    @Mock
    private AlarmParameters alarmParameters;
    
    private final MeasurementSystem measurementSystem = MeasurementSystem.METRIC;
    
    private AlarmSectorHandler alarmSectorHandler;
    private long beforeThresholdTime;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        now = System.currentTimeMillis();
        thresholdTime = now - 10 * 60 * 1000;
        beforeThresholdTime = thresholdTime - 1;
        
        alarmSectorHandler = new AlarmSectorHandler(alarmParameters);
        alarmSectorHandler.setCheckStrokeParameters(location, thresholdTime);
        
        when(alarmSector.getRanges()).thenReturn(Lists.newArrayList(alarmSectorRange1, alarmSectorRange2));
        when(alarmParameters.getMeasurementSystem()).thenReturn(measurementSystem);
        when(alarmSectorRange1.getRangeMaximum()).thenReturn(2.5f);
        when(alarmSectorRange2.getRangeMaximum()).thenReturn(5f);
    }
    
    @Test
    public void testCheckWithNullAsSector()
    {
        alarmSectorHandler.checkStroke(null, stroke);
        
        verify(location, times(0)).distanceTo(any(Location.class));
        verify(alarmParameters, times(0)).getMeasurementSystem();
    }
    
    @Test
    public void testCheckWithinThresholdTimeAndRange1()
    {
        when(stroke.getTimestamp()).thenReturn(thresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(2500f);
        
        alarmSectorHandler.checkStroke(alarmSector, stroke);
        
        verify(alarmSector, times(1)).updateClosestStrokeDistance(2.5f);
        verify(alarmSectorRange1, times(1)).getRangeMaximum();
        verify(alarmSectorRange1, times(1)).addStroke(stroke);
        verify(alarmSectorRange2, times(0)).getRangeMaximum();
    }

    @Test
    public void testCheckWithinThresholdTimeAndOutOfAllRanges()
    {
        when(stroke.getTimestamp()).thenReturn(thresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(5000.1f);

        alarmSectorHandler.checkStroke(alarmSector, stroke);

        verify(alarmSector, times(0)).updateClosestStrokeDistance(anyFloat());
        verify(alarmSectorRange1, times(1)).getRangeMaximum();
        verify(alarmSectorRange1, times(0)).addStroke(any(Stroke.class));
        verify(alarmSectorRange2, times(1)).getRangeMaximum();
        verify(alarmSectorRange2, times(0)).addStroke(any(Stroke.class));
    }

    @Test
    public void testCheckOutOfThresholdTimeAndWithinRange2()
    {
        when(stroke.getTimestamp()).thenReturn(beforeThresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(2500.1f);

        alarmSectorHandler.checkStroke(alarmSector, stroke);

        verify(alarmSector, times(0)).updateClosestStrokeDistance(anyFloat());
        verify(alarmSectorRange1, times(1)).getRangeMaximum();
        verify(alarmSectorRange1, times(0)).addStroke(any(Stroke.class));
        verify(alarmSectorRange2, times(1)).getRangeMaximum();
        verify(alarmSectorRange2, times(1)).addStroke(stroke);
    }

    @Test
    public void testCheckOutOfThresholdTimeAndAllRanges()
    {
        when(stroke.getTimestamp()).thenReturn(beforeThresholdTime);
        when(stroke.getLocation(any(Location.class))).thenReturn(strokeLocation);
        when(location.distanceTo(strokeLocation)).thenReturn(5000.1f);

        alarmSectorHandler.checkStroke(alarmSector, stroke);

        verify(alarmSector, times(0)).updateClosestStrokeDistance(anyFloat());
        verify(alarmSectorRange1, times(1)).getRangeMaximum();
        verify(alarmSectorRange1, times(0)).addStroke(any(Stroke.class));
        verify(alarmSectorRange2, times(1)).getRangeMaximum();
        verify(alarmSectorRange2, times(0)).addStroke(any(Stroke.class));
    }
}
