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
import org.blitzortung.android.data.beans.StrikeAbstract;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrikeColorHandler;
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
public class StrikesOverlayTest {

    @Implements(PreferenceManager.class)
    private static class ShadowPreferenceManager {

        @Implementation
        public static SharedPreferences getDefaultSharedPreferences(Context context) {
            return context.getSharedPreferences("", 0);
        }

    }

    private StrikesOverlay strikesOverlay;

    @Mock
    private StrikeColorHandler colorHandler;

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

        strikesOverlay = spy(new StrikesOverlay(ownMapActivity, colorHandler));

        when(ownMapActivity.getMapView()).thenReturn(ownMapView);
    }

    @Test
    public void testConstruct() {
        assertThat(strikesOverlay.size(), is(0));
    }

    @Test
    public void testAddAndExpireStrikes() {
        List<StrikeAbstract> strikes = Lists.newArrayList();

        strikesOverlay.setIntervalDuration(1);
        strikesOverlay.addStrikes(strikes);

        assertThat(strikesOverlay.size(), is(0));

        strikes.add(mock(StrikeAbstract.class));
        strikes.add(mock(StrikeAbstract.class));

        strikesOverlay.addStrikes(strikes);

        assertThat(strikesOverlay.size(), is(2));

        strikesOverlay.addStrikes(strikes);

        assertThat(strikesOverlay.size(), is(4));
    }

    @Test
    public void testClear() {
        doReturn(true).when(strikesOverlay).clearPopup();

        strikesOverlay.addStrikes(Lists.newArrayList(mock(StrikeAbstract.class)));

        strikesOverlay.clear();

        assertThat(strikesOverlay.size(), is(0));
    }

    @Test
    public void testUpdateZoomLevel() {
        strikesOverlay.updateZoomLevel(5);
        verify(strikesOverlay, times(1)).refresh();
    }

    @Test
    public void testRefresh() {
        doReturn(mock(Drawable.class)).when(strikesOverlay).updateAndReturnDrawable(any(StrikeOverlayItem.class), anyInt(), any(ColorHandler.class));

        StrikeOverlayItem strikeOverlayItem = mock(StrikeOverlayItem.class);

        strikesOverlay.strikes.add(strikeOverlayItem);

        when(strikeOverlayItem.getTimestamp()).thenReturn(System.currentTimeMillis());

        strikesOverlay.refresh();

        verify(colorHandler, times(1)).updateTarget();
        verify(colorHandler, times(1)).getColorSection(anyLong(), anyLong(), any(TimeIntervalWithOffset.class));

        verify(strikesOverlay, times(1)).updateAndReturnDrawable(eq(strikeOverlayItem), anyInt(), eq(colorHandler));
        verify(strikeOverlayItem, times(1)).setMarker(any(Drawable.class));
    }

    @Test
    public void testGetDrawable() {
        StrikeOverlayItem strikeOverlayItem = mock(StrikeOverlayItem.class);

        int section = 2;
        int color = 1234;

        when(colorHandler.getColor(section)).thenReturn(color);

        Shape drawable = strikesOverlay.updateAndReturnDrawable(strikeOverlayItem, section, colorHandler);

        assertThat(drawable, is(not(nullValue())));
    }

    @Test
    public void testGetDrawableForRaster() {
        Projection projection = mock(Projection.class);
        when(ownMapView.getProjection()).thenReturn(projection);

        RasterParameters rasterParameters = mock(RasterParameters.class);

        StrikeOverlayItem strikeOverlayItem = mock(StrikeOverlayItem.class);

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

        when(strikeOverlayItem.getPoint()).thenReturn(center);
        when(strikesOverlay.hasRasterParameters()).thenReturn(true);
        when(strikesOverlay.getRasterParameters()).thenReturn(rasterParameters);

        int section = 2;
        int color = 1234;

        when(colorHandler.getColor(section)).thenReturn(color);

        Shape drawable = strikesOverlay.updateAndReturnDrawable(strikeOverlayItem, section, colorHandler);

        verify(projection, times(1)).toPixels(eq(center), any(Point.class));
        verify(projection, times(3)).toPixels(any(GeoPoint.class), any(Point.class));

        assertThat(drawable, is(instanceOf(ShapeDrawable.class)));
    }


    @Test
    public void testCreateItem() {
        strikesOverlay.setIntervalDuration(100);
        strikesOverlay.addStrikes(Lists.newArrayList(mock(StrikeAbstract.class)));

        assertThat(strikesOverlay.size(), is(1));
        assertThat(strikesOverlay.createItem(0), is(notNullValue()));
    }

    @Test
    public void testOnTapItem() {
        long currentTime = System.currentTimeMillis();
        StrikeOverlayItem strikeOverlayItem = mock(StrikeOverlayItem.class);
        GeoPoint point = new GeoPoint(11000000, 49000000);
        when(strikeOverlayItem.getPoint()).thenReturn(point);
        when(strikeOverlayItem.getTimestamp()).thenReturn(currentTime);
        when(strikeOverlayItem.getMultiplicity()).thenReturn(1);
        when(strikeOverlayItem.getTitle()).thenReturn("<title>");

        strikesOverlay.strikes.add(strikeOverlayItem);

        doNothing().when(strikesOverlay).showPopup(any(GeoPoint.class), any(String.class));

        doReturn(false).when(strikesOverlay).clearPopup();

        strikesOverlay.onTap(0);
        // TODO returned title is <null>, as DateFormat returns a null Value here
        verify(strikesOverlay, times(1)).showPopup(point, null);
    }

    @Test
    public void testOnTapMap() {
        doReturn(false).when(strikesOverlay).clearPopup();

        strikesOverlay.onTap(mock(GeoPoint.class), mock(MapView.class));

        verify(strikesOverlay, times(1)).clearPopup();
    }
}
