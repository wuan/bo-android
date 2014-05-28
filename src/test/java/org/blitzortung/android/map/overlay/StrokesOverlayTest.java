package org.blitzortung.android.map.overlay;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.preference.PreferenceManager;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import com.google.common.collect.Lists;
import org.blitzortung.android.data.TimeIntervalWithOffset;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class StrokesOverlayTest {

    @Implements(PreferenceManager.class)
    private static class ShadowPreferenceManager {

        @Implementation
        public static SharedPreferences getDefaultSharedPreferences(Context context) {
            return context.getSharedPreferences("", 0);
        }

    }

    private StrokesOverlay strokesOverlay;

    @Mock
    private StrokeColorHandler colorHandler;

    @Mock
    private Resources resources;

    @Mock
    private OwnMapActivity ownMapActivity;

    @Mock
    private OwnMapView ownMapView;

    private final int[] colors = new int[]{1, 2, 3};

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(ownMapActivity.getResources()).thenReturn(resources);

        when(colorHandler.getColors()).thenReturn(colors);

        strokesOverlay = spy(new StrokesOverlay(ownMapActivity, colorHandler));

        when(ownMapActivity.getMapView()).thenReturn(ownMapView);
    }

    @Test
    public void testConstruct() {
        assertThat(strokesOverlay.size(), is(0));
    }

    @Test
    public void testAddAndExpireStrokes() {
        List<AbstractStroke> strokes = Lists.newArrayList();

        strokesOverlay.setIntervalDuration(1);
        strokesOverlay.addStrokes(strokes);

        assertThat(strokesOverlay.size(), is(0));

        strokes.add(mock(AbstractStroke.class));
        strokes.add(mock(AbstractStroke.class));

        strokesOverlay.addStrokes(strokes);

        assertThat(strokesOverlay.size(), is(2));

        strokesOverlay.addStrokes(strokes);

        assertThat(strokesOverlay.size(), is(4));
    }

    @Test
    public void testClear() {
        doReturn(true).when(strokesOverlay).clearPopup();

        strokesOverlay.addStrokes(Lists.newArrayList(mock(AbstractStroke.class)));

        strokesOverlay.clear();

        assertThat(strokesOverlay.size(), is(0));
    }

    @Test
    public void testUpdateZoomLevel() {
        strokesOverlay.updateZoomLevel(5);
        verify(strokesOverlay, times(1)).refresh();
    }

    @Test
    public void testRefresh() {
        doReturn(mock(Drawable.class)).when(strokesOverlay).updateAndReturnDrawable(any(StrokeOverlayItem.class), anyInt(), any(ColorHandler.class));

        StrokeOverlayItem strokeOverlayItem = mock(StrokeOverlayItem.class);

        strokesOverlay.strokes.add(strokeOverlayItem);

        when(strokeOverlayItem.getTimestamp()).thenReturn(System.currentTimeMillis());

        strokesOverlay.refresh();

        verify(colorHandler, times(1)).updateTarget();
        verify(colorHandler, times(1)).getColorSection(anyLong(), anyLong(), any(TimeIntervalWithOffset.class));

        verify(strokesOverlay, times(1)).updateAndReturnDrawable(eq(strokeOverlayItem), anyInt(), eq(colorHandler));
        verify(strokeOverlayItem, times(1)).setMarker(any(Drawable.class));
    }

    @Test
    public void testGetDrawable() {
        StrokeOverlayItem strokeOverlayItem = mock(StrokeOverlayItem.class);

        int section = 2;
        int color = 1234;

        when(colorHandler.getColor(section)).thenReturn(color);

        Shape drawable = strokesOverlay.updateAndReturnDrawable(strokeOverlayItem, section, colorHandler);

        assertThat(drawable, is(not(nullValue())));
    }

    @Test
    public void testGetDrawableForRaster() {
        Projection projection = mock(Projection.class);
        when(ownMapView.getProjection()).thenReturn(projection);

        RasterParameters rasterParameters = mock(RasterParameters.class);

        StrokeOverlayItem strokeOverlayItem = mock(StrokeOverlayItem.class);

        GeoPoint center = mock(GeoPoint.class);
        when(center.getLatitudeE6()).thenReturn(49000000);
        when(center.getLongitudeE6()).thenReturn(11000000);

        Point centerPoint = mock(Point.class);
        Point topLeftPoint = mock(Point.class);
        Point bottomRightPoint = mock(Point.class);
        when(projection.toPixels(any(GeoPoint.class), any(Point.class)))
                .thenReturn(centerPoint)
                .thenReturn(topLeftPoint)
                .thenReturn(bottomRightPoint);

        when(strokeOverlayItem.getPoint()).thenReturn(center);
        when(strokesOverlay.hasRasterParameters()).thenReturn(true);
        when(strokesOverlay.getRasterParameters()).thenReturn(rasterParameters);

        int section = 2;
        int color = 1234;

        when(colorHandler.getColor(section)).thenReturn(color);

        Shape drawable = strokesOverlay.updateAndReturnDrawable(strokeOverlayItem, section, colorHandler);

        verify(projection, times(1)).toPixels(eq(center), any(Point.class));
        verify(projection, times(3)).toPixels(any(GeoPoint.class), any(Point.class));

        assertThat(drawable, is(instanceOf(ShapeDrawable.class)));
    }


    @Test
    public void testCreateItem() {
        strokesOverlay.setIntervalDuration(100);
        strokesOverlay.addStrokes(Lists.newArrayList(mock(AbstractStroke.class)));

        assertThat(strokesOverlay.size(), is(1));
        assertThat(strokesOverlay.createItem(0), is(notNullValue()));
    }

    @Test
    public void testOnTapItem() {
        long currentTime = System.currentTimeMillis();
        StrokeOverlayItem strokeOverlayItem = mock(StrokeOverlayItem.class);
        GeoPoint point = new GeoPoint(11000000, 49000000);
        when(strokeOverlayItem.getPoint()).thenReturn(point);
        when(strokeOverlayItem.getTimestamp()).thenReturn(currentTime);
        when(strokeOverlayItem.getMultiplicity()).thenReturn(1);
        when(strokeOverlayItem.getTitle()).thenReturn("<title>");

        strokesOverlay.strokes.add(strokeOverlayItem);

        doNothing().when(strokesOverlay).showPopup(any(GeoPoint.class), any(String.class));

        doReturn(false).when(strokesOverlay).clearPopup();

        strokesOverlay.onTap(0);
        // TODO returned title is <null>, as DateFormat returns a null Value here
        verify(strokesOverlay, times(1)).showPopup(point, null);
    }

    @Test
    public void testOnTapMap() {
        doReturn(false).when(strokesOverlay).clearPopup();

        strokesOverlay.onTap(mock(GeoPoint.class), mock(MapView.class));

        verify(strokesOverlay, times(1)).clearPopup();
    }
}
