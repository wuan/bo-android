package org.blitzortung.android.map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19)
public class OwnMapActivityTest {

    private OwnMapActivityForTest ownMapActivity;
    @Mock
    private OwnMapView ownMapView;
    private LayoutInflater layoutInflater = new LayoutInflater(RuntimeEnvironment.application.getApplicationContext()) {
        @Override
        public LayoutInflater cloneInContext(Context context) {
            return null;
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ownMapActivity = spy(new OwnMapActivityForTest());

        ownMapActivity.setMapView(ownMapView);
    }

    @Test
    public void testGetPopupInflatesLayoutOnlyOnce() {
        View popUp = ownMapView.getPopup();

        assertThat(ownMapView.getPopup(), is(sameInstance(popUp)));
    }

    @Test
    public void testGetMapView() {
        assertThat(ownMapActivity.getMapView(), is(sameInstance(ownMapView)));
    }

    private static class OwnMapActivityForTest extends OwnMapActivity {
        @Override
        protected boolean isRouteDisplayed() {
            return false;
        }
    }

}
