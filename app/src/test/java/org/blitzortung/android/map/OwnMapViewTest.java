package org.blitzortung.android.map;

import android.content.Context;
import android.graphics.Canvas;

import com.google.android.maps.Projection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19)
public class OwnMapViewTest {

    @Mock
    Context context;
    @Mock
    Projection projection;
    @Mock
    OwnMapView.ZoomListener zoomListener;
    private OwnMapView ownMapView;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        ownMapView = spy(new OwnMapView(context, "<apiKey>"));

        ownMapView.addZoomListener(zoomListener);

        when(ownMapView.getProjection()).thenReturn(projection);
    }

    @Test
    public void testZoomLevelArgumentToZoomListener() {
        when(ownMapView.getZoomLevel()).thenReturn(42);
        ownMapView.detectAndHandleZoomAction();

        ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(zoomListener, times(1)).onZoom(argumentCaptor.capture());

        Assert.assertThat(argumentCaptor.getValue(), is(42));
    }

    @Test
    public void testNoZoomDetectionAtOnTouchEventAtFixedScale() {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.detectAndHandleZoomAction();
        verify(zoomListener, times(1)).onZoom(anyInt());

        ownMapView.detectAndHandleZoomAction();
        verify(zoomListener, times(1)).onZoom(anyInt());
    }

    @Test
    public void testZoomDetectionAtOnTouchEventAtChangingScale() {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.detectAndHandleZoomAction();
        verify(zoomListener, times(1)).onZoom(anyInt());

        when(projection.metersToEquatorPixels(1000f)).thenReturn(10f);
        ownMapView.detectAndHandleZoomAction();
        verify(zoomListener, times(2)).onZoom(anyInt());
    }

    @Ignore
    public void testNoZoomDetectionAtDispatchDrawAtFixedScale() {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(1)).onZoom(anyInt());

        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(1)).onZoom(anyInt());
    }

    @Ignore
    public void testZoomDetectionAtDispatchDrawAtChangingScale() {
        when(projection.metersToEquatorPixels(1000f)).thenReturn(5f);
        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(1)).onZoom(anyInt());

        when(projection.metersToEquatorPixels(1000f)).thenReturn(10f);
        ownMapView.dispatchDraw(mock(Canvas.class));
        verify(zoomListener, times(2)).onZoom(anyInt());
    }
}
