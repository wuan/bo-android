package org.blitzortung.android.map;

import android.view.LayoutInflater;
import android.view.View;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.app.R;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class OwnMapActivityTest {

    private static class OwnMapActivityForTest extends OwnMapActivity
    {
        @Override
        protected boolean isRouteDisplayed() {
            return false;
        }
    }

    private OwnMapActivityForTest ownMapActivity;

    @Mock
    private OwnMapView ownMapView;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        ownMapActivity = spy(new OwnMapActivityForTest());

        ownMapActivity.setMapView(ownMapView);
    }

    @Test
    public void testGetPopupInflatesLayoutOnlyOnce()
    {
        LayoutInflater layoutInflater = mock(LayoutInflater.class);
        View popUp = mock(View.class);

        when(ownMapActivity.getLayoutInflater()).thenReturn(layoutInflater);
        when(layoutInflater.inflate(R.layout.popup, ownMapView, false)).thenReturn(popUp);

        assertThat(ownMapView.getPopup(), is(sameInstance(popUp)));
        verify(layoutInflater, times(1)).inflate(R.layout.popup, ownMapView, false);

        assertThat(ownMapView.getPopup(), is(sameInstance(popUp)));
        verify(layoutInflater, times(1)).inflate(R.layout.popup, ownMapView, false);
    }

    @Test
    public void testGetMapView()
    {
        assertThat(ownMapActivity.getMapView(), is(sameInstance(ownMapView)));
    }

}
