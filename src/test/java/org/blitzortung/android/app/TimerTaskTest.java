package org.blitzortung.android.app;

import android.content.SharedPreferences;
import android.content.res.Resources;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.DataRetriever;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TimerTaskTest {

    private TimerTask timerTask;

    private Resources resources;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private DataRetriever dataRetriever;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        resources = Robolectric.application.getResources();

        when(sharedPreferences.getString(Preferences.QUERY_PERIOD_KEY, "60")).thenReturn("60");
        when(sharedPreferences.getString(Preferences.BACKGROUND_QUERY_PERIOD_KEY, "0")).thenReturn("0");

        when(dataRetriever.getMinutes()).thenReturn(60);

        timerTask = spy(new TimerTask(resources, sharedPreferences, dataRetriever));
    }

    @After
    public void tearDown()
    {
        timerTask.disable();
    }

    @Test
    public void testConstruct()
    {
        verify(sharedPreferences, times(1)).registerOnSharedPreferenceChangeListener(any(TimerTask.class));
        verify(sharedPreferences, times(1)).getString(Preferences.QUERY_PERIOD_KEY, "60");
        verify(sharedPreferences, times(1)).getString(Preferences.BACKGROUND_QUERY_PERIOD_KEY, "0");

        assertThat(timerTask.getPeriod(), is(60));
        assertThat(timerTask.getBackgroundPeriod(), is(0));
    }

    @Test
    public void testRun()
    {
        long actualSecond = System.currentTimeMillis() / 1000;

        timerTask.run();

        ArgumentCaptor<DataRetriever.UpdateTargets> targetsArgumentCaptor = ArgumentCaptor.forClass(DataRetriever.UpdateTargets.class);
        verify(dataRetriever, times(1)).updateData(targetsArgumentCaptor.capture());

        assertTrue(targetsArgumentCaptor.getValue().anyUpdateRequested());
        assertTrue(targetsArgumentCaptor.getValue().updateStrokes());
        assertTrue(targetsArgumentCaptor.getValue().updateParticipants());

        assertThat(timerTask.getLastUpdate(), is(actualSecond));
        assertThat(timerTask.getLastParticipantsUpdate(), is(actualSecond));
    }

    class TimerListener implements TimerTask.StatusListener
    {
        private String statusString;

        @Override
        public void onStatusUpdate(String statusString) {
            this.statusString = statusString;
        }

        public String getStatusString() {
            return statusString;
        }
    }

    @Test
    public void testRunWithListenerSet()
    {
        TimerListener listener = new TimerListener();

        timerTask.setListener(listener);
        timerTask.run();

        assertThat(listener.getStatusString(), is("no stroke/60 minutes 60/60s"));
    }

    @Test
    public void testRunWithListenerAndStrokeNumberSet()
    {
        TimerListener listener = new TimerListener();

        timerTask.setListener(listener);
        timerTask.setNumberOfStrokes(1234);
        timerTask.run();

        assertThat(listener.getStatusString(), is("1234 strokes/60 minutes 60/60s"));
    }

    @Test
    public void testRunWithRasterAndListenerSet()
    {
        when(dataRetriever.isUsingRaster()).thenReturn(true);
        when(dataRetriever.getRegion()).thenReturn(3);
        TimerListener listener = new TimerListener();

        timerTask.setListener(listener);
        timerTask.run();

        assertThat(listener.getStatusString(), is("no stroke/60 minutes 60/60s USA"));
    }

    @Test
    public void testRestart() {
        timerTask.run();

        timerTask.restart();

        assertThat(timerTask.getLastUpdate(), is(0l));
        assertThat(timerTask.getLastParticipantsUpdate(), is(0l));
    }

    @Test
         public void testOnResume() {
        long actualSecond = System.currentTimeMillis() / 1000;

        timerTask.onResume();

        assertThat(timerTask.getLastUpdate(), is(actualSecond));
        assertThat(timerTask.getLastParticipantsUpdate(), is(actualSecond));
        assertFalse(timerTask.isInBackgroundOperation());
    }

    @Test
    public void testOnPause() {
        long actualSecond = System.currentTimeMillis() / 1000;

        timerTask.onResume();

        timerTask.onPause();

        assertTrue(timerTask.isInBackgroundOperation());
    }

    @Test
    public void testSetAlarmEnabled() {
        timerTask.setAlarmEnabled(true);
        assertTrue(timerTask.getAlarmEnabled());

        timerTask.setAlarmEnabled(false);
        assertFalse(timerTask.getAlarmEnabled());

    }
}

