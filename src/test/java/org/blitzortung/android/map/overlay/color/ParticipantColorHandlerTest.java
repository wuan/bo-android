package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.intThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.when;

public class ParticipantColorHandlerTest {
    @Mock
    private SharedPreferences sharedPreferences;

    private ParticipantColorHandler participantColorHandler;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(sharedPreferences.getString("map_mode", "SATELLITE")).thenReturn("SATELLITE");

        participantColorHandler = new ParticipantColorHandler(sharedPreferences);
    }

    @Test
    public void testSatelliteColors()
    {
        int[] colors = participantColorHandler.getColors(ColorTarget.SATELLITE);

        assertThat(colors.length, is(3));
        assertThat(colors, is(new int[]{0xff88ff22, 0xffff9900, 0xffff0000}));
    }

    @Test
    public void testMapColors()
    {
        int[] colors = participantColorHandler.getColors(ColorTarget.STREETMAP);

        assertThat(colors.length, is(3));
        assertThat(colors, is(new int[]{0xff448811, 0xff884400, 0xff880000}));
    }
}
