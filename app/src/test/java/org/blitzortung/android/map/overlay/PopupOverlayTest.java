package org.blitzortung.android.map.overlay;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

import org.blitzortung.android.app.BuildConfig;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.OwnMapActivity;
import org.blitzortung.android.map.OwnMapView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19, constants=BuildConfig.class)
public class PopupOverlayTest {

    class PopupOverlayItemForTest extends OverlayItem
    {
        public PopupOverlayItemForTest(GeoPoint geoPoint, String s, String s1) {
            super(geoPoint, s, s1);
        }
    }

    class PopupOverlayForTest extends PopupOverlay<PopupOverlayItemForTest>
    {
        public PopupOverlayForTest(OwnMapActivity mapActivity, Drawable defaultMarker)
        {
            super(mapActivity, defaultMarker);
        }

        @Override
        protected PopupOverlayItemForTest createItem(int i) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    private PopupOverlayForTest popupOverlay;

    @Mock
    private OwnMapActivity activity;

    @Before
    public void setUp()
    {
        popupOverlay = spy(new PopupOverlayForTest(activity, mock(Drawable.class)));
    }

    @Test
    public void testSetGetActivity()
    {
        activity = mock(OwnMapActivity.class);

        assertThat(popupOverlay.getActivity(), is(activity));
    }

    @Test
    public void testShowPopup()
    {
        OwnMapView mapView = mock(OwnMapView.class);
        when(activity.getMapView()).thenReturn(mapView);

        View popUp = mock(View.class);
        when(mapView.getPopup()).thenReturn(popUp);

        TextView statusText = mock(TextView.class);
        when(popUp.findViewById(R.id.popup_text)).thenReturn(statusText);

        GeoPoint location = mock(GeoPoint.class);

        popupOverlay.showPopup(location, "<title>");

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        verify(statusText, times(1)).setText(title.capture());
        assertThat(title.getValue(), is("<title>"));

        verify(mapView, times(1)).addView(eq(popUp), any(MapView.LayoutParams.class));

        assertTrue(popupOverlay.popupShown);
    }

    @Test
    public void testClearPopup()
    {
        OwnMapView mapView = mock(OwnMapView.class);
        when(activity.getMapView()).thenReturn(mapView);

        View popUp = mock(View.class);
        when(mapView.getPopup()).thenReturn(popUp);

        assertFalse(popupOverlay.clearPopup());

        verify(mapView, times(1)).removeView(popUp);

        assertFalse(popupOverlay.popupShown);

        popupOverlay.popupShown = true;

        assertTrue(popupOverlay.clearPopup());

        assertFalse(popupOverlay.popupShown);
    }
}
