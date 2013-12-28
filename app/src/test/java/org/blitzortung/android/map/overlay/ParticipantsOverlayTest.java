package org.blitzortung.android.map.overlay;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.common.collect.Lists;
import org.blitzortung.android.data.beans.Station;
import org.blitzortung.android.map.color.ParticipantColorHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class ParticipantsOverlayTest {

    @Implements(PreferenceManager.class)
    private static class ShadowPreferenceManager {

        @Implementation
        public static SharedPreferences getDefaultSharedPreferences(Context context) {
            return context.getSharedPreferences("", 0);
        }

    }
    private ParticipantsOverlay participantsOverlay;

    @Mock
    private Context context;

    @Mock
    private Resources resources;

    @Mock
    private ParticipantColorHandler colorHandler;

    private final int[] colors = new int[]{1,2,3};

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(colorHandler.getColors()).thenReturn(colors);
        when(context.getResources()).thenReturn(resources);

        participantsOverlay = spy(new ParticipantsOverlay(context, colorHandler));
    }

    @Test
    public void testConstruct()
    {
        assertThat(participantsOverlay.size(), is(0));
    }

    @Test
    public void testSetParticipants()
    {
        List<Station> stations = Lists.newArrayList();

        participantsOverlay.setParticipants(stations);

        assertThat(participantsOverlay.size(), is(0));

        stations.add(mock(Station.class));
        stations.add(mock(Station.class));

        participantsOverlay.setParticipants(stations);

        assertThat(participantsOverlay.size(), is(2));

        participantsOverlay.setParticipants(stations);

        assertThat(participantsOverlay.size(), is(2));
    }

    @Test
    public void testClear()
    {
        participantsOverlay.setParticipants(Lists.newArrayList(mock(Station.class)));

        participantsOverlay.clear();

        assertThat(participantsOverlay.size(), is(0));
    }

    @Test
    public void testUpdateZoomLevel()
    {
        participantsOverlay.updateZoomLevel(-10);
        assertThat(participantsOverlay.shapeSize, is(1));
        verify(participantsOverlay, times(1)).refresh();

        participantsOverlay.updateZoomLevel(4);
        assertThat(participantsOverlay.shapeSize, is(1));

        participantsOverlay.updateZoomLevel(5);
        assertThat(participantsOverlay.shapeSize, is(2));

        participantsOverlay.updateZoomLevel(6);
        assertThat(participantsOverlay.shapeSize, is(3));
    }

    @Test
    public void testRefresh()
    {
        ParticipantOverlayItem participantOverlayItem = mock(ParticipantOverlayItem.class);

        participantsOverlay.items.add(participantOverlayItem);

        when(participantOverlayItem.getState()).thenReturn(Station.State.ON);

        participantsOverlay.refresh();

        verify(colorHandler, times(1)).getColors();
        verify(participantsOverlay, times(1)).getDrawable(colors[0]);
        verify(participantsOverlay, times(1)).getDrawable(colors[1]);
        verify(participantsOverlay, times(1)).getDrawable(colors[2]);
        verify(participantOverlayItem, times(1)).getState();
        verify(participantOverlayItem, times(1)).setMarker(any(Drawable.class));
    }

    @Test
    public void testCreateItem()

    {
        participantsOverlay.setParticipants(Lists.newArrayList(mock(Station.class)));

        assertThat(participantsOverlay.createItem(0), is(notNullValue()));
    }

    @Test
    public void testOnTapItem()
    {
        ParticipantOverlayItem participantOverlayItem = mock(ParticipantOverlayItem.class);
        GeoPoint point = new GeoPoint(11000000,49000000);
        when(participantOverlayItem.getPoint()).thenReturn(point);
        when(participantOverlayItem.getTitle()).thenReturn("<title>");

        participantsOverlay.items.add(participantOverlayItem);

        doNothing().when(participantsOverlay).showPopup(any(GeoPoint.class), any(String.class));

        participantsOverlay.onTap(0);
        verify(participantsOverlay, times(1)).showPopup(point, "<title>");
    }

    @Test
    public void testOnTapMap()
    {
        doReturn(false).when(participantsOverlay).clearPopup();

        participantsOverlay.onTap(mock(GeoPoint.class), mock(MapView.class));

        verify(participantsOverlay, times(1)).clearPopup();
    }
}
