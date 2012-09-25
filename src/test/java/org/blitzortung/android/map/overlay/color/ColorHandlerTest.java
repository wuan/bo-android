package org.blitzortung.android.map.overlay.color;

import android.content.SharedPreferences;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class ColorHandlerTest {

    static class ColorHandlerForTest extends ColorHandler {

        public ColorHandlerForTest(SharedPreferences sharedPreferences)
        {
            super(sharedPreferences);
        }

        @Override
        protected int[] getColors(ColorTarget target) {
            return new int[0];
        }
    }

    private ColorHandler colorHandler;

    @Mock
    SharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(sharedPreferences.getString("map_mode", ColorTarget.SATELLITE.toString())).thenReturn(ColorTarget.SATELLITE.toString());

        colorHandler = spy(new ColorHandlerForTest(sharedPreferences));
    }

    @Test
    public void testGetColors()
    {
        int[] colors = new int[]{1, 2, 3, 4, 5};
        when(colorHandler.getColors()).thenReturn(colors);

        assertThat(colorHandler.getColors(), is(colors));

        verify(colorHandler, times(1)).getColors(ColorTarget.SATELLITE);
    }

    @Test
    public void testGetColorsForAlternativeMap()
    {
        when(sharedPreferences.getString("map_mode", ColorTarget.SATELLITE.toString())).thenReturn(ColorTarget.STREETMAP.toString());
        colorHandler = spy(new ColorHandlerForTest(sharedPreferences));

        int[] colors = new int[]{1, 2, 3, 4, 5};
        when(colorHandler.getColors()).thenReturn(colors);

        assertThat(colorHandler.getColors(), is(colors));

        verify(colorHandler, times(1)).getColors(ColorTarget.STREETMAP);
    }

    @Test
    public void testGetColor()
    {
        when(colorHandler.getColors()).thenReturn(new int[]{1, 2, 3, 4, 5});

        long now = System.currentTimeMillis();
        int minutesPerColor = 10;
        long timePerColor = minutesPerColor * 60 * 1000;

        assertThat(colorHandler.getColor(now, now, minutesPerColor), is(1));
        assertThat(colorHandler.getColor(now, now - timePerColor + 1, minutesPerColor), is(1));

        assertThat(colorHandler.getColor(now, now - timePerColor, minutesPerColor), is(2));
        assertThat(colorHandler.getColor(now, now - 2 * timePerColor + 1, minutesPerColor), is(2));

        assertThat(colorHandler.getColor(now, now - 2 * timePerColor, minutesPerColor), is(3));
        assertThat(colorHandler.getColor(now, now - 3 * timePerColor + 1, minutesPerColor), is(3));

        assertThat(colorHandler.getColor(now, now - 3 * timePerColor, minutesPerColor), is(4));
        assertThat(colorHandler.getColor(now, now - 4 * timePerColor + 1, minutesPerColor), is(4));

        assertThat(colorHandler.getColor(now, now - 4 * timePerColor, minutesPerColor), is(5));
        assertThat(colorHandler.getColor(now, now - 5 * timePerColor + 1, minutesPerColor), is(5));

        assertThat(colorHandler.getColor(now, now - 5 * timePerColor, minutesPerColor), is(5));
    }

    @Test
    public void testGetTextColor()
    {
        assertThat(colorHandler.getTextColor(), is(0xffffffff));

        verify(colorHandler, times(1)).getTextColor(ColorTarget.SATELLITE);
    }

    @Test
    public void testGetTextColorForAlternativeMap()
    {
        when(sharedPreferences.getString("map_mode", ColorTarget.SATELLITE.toString())).thenReturn(ColorTarget.STREETMAP.toString());
        colorHandler = spy(new ColorHandlerForTest(sharedPreferences));

        assertThat(colorHandler.getTextColor(), is(0xffffffff));

        verify(colorHandler, times(1)).getTextColor(ColorTarget.STREETMAP);
    }
}
