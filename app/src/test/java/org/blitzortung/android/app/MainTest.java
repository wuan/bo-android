package org.blitzortung.android.app;

import org.blitzortung.android.app.view.components.StatusComponent;
import org.blitzortung.android.map.overlay.StrikesOverlay;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19)
public class MainTest {

    private Main main;

    @Mock
    private StatusComponent statusComponent;

    @Mock
    private StrikesOverlay strikesOverlay;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        main = new Main();
        main.statusComponent = statusComponent;
        main.strikesOverlay = strikesOverlay;
    }

    @Test
    public void testCreateStatusText() {
        when(strikesOverlay.getTotalNumberOfStrikes()).thenReturn(0);

        main.setStatusString("foo");

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(statusComponent, times(1)).setText(argument.capture());

        assertThat(argument.getValue(), is("no strike/0 minutes foo"));
    }


    @Test
    public void testCreateStatusTextWithStrikeNumberSet() {
        when(strikesOverlay.getTotalNumberOfStrikes()).thenReturn(1234);

        main.setStatusString("foo");

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(statusComponent, times(1)).setText(argument.capture());

        assertThat(argument.getValue(), is("1234 strikes/0 minutes foo"));
    }

    @Test
    public void testRunWithRasterAndListenerSet() {
        when(strikesOverlay.hasRasterParameters()).thenReturn(true);

        main.setStatusString("foo");

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(statusComponent, times(1)).setText(argument.capture());

        assertThat(argument.getValue(), is("no strike/0 minutes foo"));
    }
}
