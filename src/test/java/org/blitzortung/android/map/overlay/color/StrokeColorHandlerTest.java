package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.app.Preferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class StrokeColorHandlerTest {
    @Mock
    private SharedPreferences sharedPreferences;

    private StrokeColorHandler strokeColorHandler;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(sharedPreferences.getString(Preferences.MAP_TYPE_KEY, ColorTarget.SATELLITE.toString())).thenReturn(ColorTarget.SATELLITE.toString());
        when(sharedPreferences.getString(Preferences.COLOR_SCHEME_KEY, ColorScheme.BLITZORTUNG.toString())).thenReturn(ColorScheme.BLITZORTUNG.toString());

        strokeColorHandler = spy(new StrokeColorHandler(sharedPreferences));
    }

    @Test
    public void testSatelliteColors()
    {
        int[] colors = strokeColorHandler.getColors(ColorTarget.SATELLITE);

        assertThat(colors.length, is(6));
        assertThat(colors, is(new int[]{0xffe4f9f9, 0xffd8f360, 0xffdfbc51, 0xffe48044, 0xffe73c3b, 0xffb82e2d}));
    }

    @Test
    public void testMapColors()
    {
        int[] colors = strokeColorHandler.getColors(ColorTarget.STREETMAP);

        assertThat(colors.length, is(6));
        assertThat(colors, is(new int[]{0, 0, 0, 0, 0, 0}));

        verify(strokeColorHandler, times(1)).modifyBrightness(eq(new int[]{0xffe4f9f9, 0xffd8f360, 0xffdfbc51, 0xffe48044, 0xffe73c3b, 0xffb82e2d}), anyFloat());
    }
}
