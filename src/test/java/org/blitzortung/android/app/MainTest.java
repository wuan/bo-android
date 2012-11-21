package org.blitzortung.android.app;

import android.widget.TextView;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
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
        main = new Main();
        main.status = status;
        main.strokesOverlay = strokesOverlay;
    }

    public void testCreateStatusText() {
        when(strokesOverlay.getTotalNumberOfStrokes()).thenReturn(0);

        main.setStatusString("foo");


        assertThat(main.setStatusString("foo"), is("no stroke/60 minutes 60/60s"));
    }
}
