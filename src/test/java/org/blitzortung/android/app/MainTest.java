package org.blitzortung.android.app;

import android.widget.TextView;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MainTest {

    private Main main;

    @Mock
    private TextView status;

    @Mock
    private StrokesOverlay strokesOverlay;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        main = new Main();
        main.status = status;
        main.strokesOverlay = strokesOverlay;
    }

    @Test
    public void testCreateStatusText() {
        when(strokesOverlay.getTotalNumberOfStrokes()).thenReturn(0);

        main.setStatusString("foo");

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(status, times(1)).setText(argument.capture());

        assertThat(argument.getValue(), is("no stroke/0 minutes foo"));
    }


    @Test
    public void testCreateStatusTextWithStrokeNumberSet()
    {
        when(strokesOverlay.getTotalNumberOfStrokes()).thenReturn(1234);

        main.setStatusString("foo");

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(status, times(1)).setText(argument.capture());

        assertThat(argument.getValue(), is("1234 strokes/0 minutes foo"));
    }

    @Test
    public void testRunWithRasterAndListenerSet()
    {
        when(strokesOverlay.getRasterParameters()).thenReturn(true);
        when(strokesOverlay.getRegion()).thenReturn(3);

        main.setStatusString("foo");

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(status, times(1)).setText(argument.capture());

        assertThat(argument.getValue(), is("no stroke/0 minutes foo USA"));
    }
}
