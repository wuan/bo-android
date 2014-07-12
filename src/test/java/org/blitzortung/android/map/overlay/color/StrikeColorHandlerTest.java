package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;
import org.blitzortung.android.app.view.PreferenceKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class StrikeColorHandlerTest {
    @Mock
    private SharedPreferences sharedPreferences;

    private StrikeColorHandler strikeColorHandler;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(sharedPreferences.getString(PreferenceKey.MAP_TYPE.toString(), ColorTarget.SATELLITE.toString())).thenReturn(ColorTarget.SATELLITE.toString());
        when(sharedPreferences.getString(PreferenceKey.COLOR_SCHEME.toString(), ColorScheme.BLITZORTUNG.toString())).thenReturn(ColorScheme.BLITZORTUNG.toString());

        strikeColorHandler = spy(new StrikeColorHandler(sharedPreferences));
    }

    @Test
    public void testSatelliteColors()
    {
        int[] colors = strikeColorHandler.getColors(ColorTarget.SATELLITE);

        assertThat(colors.length, is(6));
        assertThat(colors, is(new int[]{0xffe4f9f9, 0xffd8f360, 0xffdfbc51, 0xffe48044, 0xffe73c3b, 0xffb82e2d}));
    }

    @Test
    public void testMapColors()
    {
        int[] colors = strikeColorHandler.getColors(ColorTarget.STREETMAP);

        assertThat(colors.length, is(6));
        assertThat(colors, is(new int[]{0, 0, 0, 0, 0, 0}));

        verify(strikeColorHandler, times(1)).modifyBrightness(eq(new int[]{0xffe4f9f9, 0xffd8f360, 0xffdfbc51, 0xffe48044, 0xffe73c3b, 0xffb82e2d}), anyFloat());
    }
}
