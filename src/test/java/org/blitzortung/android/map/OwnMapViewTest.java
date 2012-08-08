package org.blitzortung.android.map;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import com.google.android.maps.Projection;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class OwnMapViewTest {

    private OwnMapView ownMapView;

    @Mock
    Context context;

    @Mock
    Projection projection;

    @Mock
    OwnMapView.ZoomListener zoomListener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        ownMapView = spy(new OwnMapView(context, "<apiKey>"));

        ownMapView.addZoomListener(zoomListener);

        when(ownMapView.getProjection()).thenReturn(projection);
    }

    @Test
    public void testZoomLevelArgumentToZoomListener()
    {
        when(ownMapView.getZoomLevel()).thenReturn(42);
        ownMapView.onTouchEvent(mock(MotionEvent.class));

        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(zoomListener, times(1)).onZoom(argumentCaptor.capture());

        Assert.assertThat(argumentCaptor.getValue(), is(42));
    }

    @Test
    public void testNoZoomDetectionAtOnTouchEventAtFixedScale()
    {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.onTouchEvent(mock(MotionEvent.class));
        verify(zoomListener, times(1)).onZoom(anyInt());

        ownMapView.onTouchEvent(mock(MotionEvent.class));
        verify(zoomListener, times(1)).onZoom(anyInt());
    }

    @Test
    public void testZoomDetectionAtOnTouchEventAtChangingScale()
    {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.onTouchEvent(mock(MotionEvent.class));
        verify(zoomListener, times(1)).onZoom(anyInt());

        when(projection.metersToEquatorPixels(1000f)).thenReturn(10f);
        ownMapView.onTouchEvent(mock(MotionEvent.class));
        verify(zoomListener, times(2)).onZoom(anyInt());
    }

    @Test
    public void testNoZoomDetectionAtDispatchDrawAtFixedScale()
    {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(1)).onZoom(anyInt());

        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(1)).onZoom(anyInt());
    }

    @Test
    public void testZoomDetectionAtDispatchDrawAtChangingScale()
    {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(1)).onZoom(anyInt());

        when(projection.metersToEquatorPixels(1000f)).thenReturn(10f);
        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(2)).onZoom(anyInt());
    }
}
