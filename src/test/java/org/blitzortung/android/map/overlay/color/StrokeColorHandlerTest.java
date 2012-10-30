package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class StrokeColorHandlerTest {
    @Mock
    private SharedPreferences sharedPreferences;

    private StrokeColorHandler strokeColorHandler;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(sharedPreferences.getString("map_mode", "SATELLITE")).thenReturn("SATELLITE");

        strokeColorHandler = new StrokeColorHandler(sharedPreferences);
    }

    @Test
    public void testSatelliteColors()
    {
        int[] colors = strokeColorHandler.getColors(ColorTarget.SATELLITE);

        assertThat(colors.length, is(6));
        assertThat(colors, is(new int[]{0xffff0000, 0xffff9900, 0xffffff00, 0xff99ff22, 0xff00ffff, 0xff6699ff}));
    }

    @Test
    public void testMapColors()
    {
        int[] colors = strokeColorHandler.getColors(ColorTarget.STREETMAP);

        assertThat(colors.length, is(6));
        assertThat(colors, is(new int[]{0xffcc2200, 0xffaa5500, 0xffaaaa00, 0xff44aa11, 0xff00aaaa, 0xff2222cc }));
    }
}
