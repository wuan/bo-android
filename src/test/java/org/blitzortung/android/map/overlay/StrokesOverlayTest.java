package org.blitzortung.android.map.overlay;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.common.collect.Lists;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import org.blitzortung.android.data.TimeIntervalWithOffset;
import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.map.overlay.color.ColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class StrokesOverlayTest {

    @Implements(PreferenceManager.class)
    static class ShadowPreferenceManager {

        @Implementation
        public static SharedPreferences getDefaultSharedPreferences(Context context) {
            return context.getSharedPreferences("", 0);
        }

    }
    private StrokesOverlay strokesOverlay;

    @Mock
    private StrokeColorHandler colorHandler;

    private int colors[] = new int[]{1,2,3};

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(colorHandler.getColors()).thenReturn(colors);

        strokesOverlay = spy(new StrokesOverlay(colorHandler));
    }

    @Test
    public void testConstruct()
    {
        assertThat(strokesOverlay.size(), is(0));
    }

    @Test
    public void testAddAndExpireStrokes()
    {
        doNothing().when(strokesOverlay).expireStrokes(anyLong());

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
    public void testClear()
    {
        doReturn(true).when(strokesOverlay).clearPopup();

        strokesOverlay.addStrokes(Lists.newArrayList(mock(AbstractStroke.class)));

        strokesOverlay.clear();

        assertThat(strokesOverlay.size(), is(0));
    }

    @Test
    public void testUpdateZoomLevel()
    {
        strokesOverlay.updateZoomLevel(5);
        verify(strokesOverlay, times(1)).refresh();
    }

    @Test
    public void testRefresh()
    {
        doReturn(mock(Drawable.class)).when(strokesOverlay).getDrawable(any(StrokeOverlayItem.class), anyInt(), any(ColorHandler.class));

        StrokeOverlayItem strokeOverlayItem = mock(StrokeOverlayItem.class);

        strokesOverlay.items.add(strokeOverlayItem);

        when(strokeOverlayItem.getTimestamp()).thenReturn(System.currentTimeMillis());

        strokesOverlay.refresh();

        verify(colorHandler, times(1)).updateTarget();
        verify(colorHandler, times(1)).getColorSection(anyLong(), anyLong(), any(TimeIntervalWithOffset.class));

        verify(strokesOverlay, times(1)).getDrawable(eq(strokeOverlayItem), anyInt(), eq(colorHandler));
        verify(strokeOverlayItem, times(1)).setMarker(any(Drawable.class));
    }

    @Test
    public void testCreateItem()
    {
        doNothing().when(strokesOverlay).expireStrokes(anyLong());
        strokesOverlay.setIntervalDuration(100);
        strokesOverlay.addStrokes(Lists.newArrayList(mock(AbstractStroke.class)));

        assertThat(strokesOverlay.size(), is(1));
        assertThat(strokesOverlay.createItem(0), is(notNullValue()));
    }

    @Test
    public void testOnTapItem()
    {
        long currentTime = System.currentTimeMillis();
        StrokeOverlayItem strokeOverlayItem = mock(StrokeOverlayItem.class);
        GeoPoint point = new GeoPoint(11000000,49000000);
        when(strokeOverlayItem.getPoint()).thenReturn(point);
        when(strokeOverlayItem.getTimestamp()).thenReturn(currentTime);
        when(strokeOverlayItem.getMultiplicity()).thenReturn(1);
        when(strokeOverlayItem.getTitle()).thenReturn("<title>");

        strokesOverlay.items.add(strokeOverlayItem);

        doNothing().when(strokesOverlay).showPopup(any(GeoPoint.class), any(String.class));

        doReturn(false).when(strokesOverlay).clearPopup();

        strokesOverlay.onTap(0);
        // TODO returned title is <null>, as DateFormat returns a null Value here
        verify(strokesOverlay, times(1)).showPopup(point, null);
    }

    @Test
    public void testOnTapMap()
    {
        doReturn(false).when(strokesOverlay).clearPopup();

        strokesOverlay.onTap(mock(GeoPoint.class), mock(MapView.class));

        verify(strokesOverlay, times(1)).clearPopup();
    }
}
