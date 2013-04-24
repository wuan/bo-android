package org.blitzortung.android.app;

import android.content.SharedPreferences;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TimerTaskTest {

    private TimerTask timerTask;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private DataHandler dataHandler;
    
    @Captor
    ArgumentCaptor<Set<DataChannel>> dataChannelsCaptor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(sharedPreferences.getString(PreferenceKey.QUERY_PERIOD.toString(), "60")).thenReturn("60");
        when(sharedPreferences.getString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "0")).thenReturn("0");

        when(dataHandler.getIntervalDuration()).thenReturn(60);

        timerTask = spy(new TimerTask(sharedPreferences, dataHandler));
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
        verify(sharedPreferences, times(1)).getString(PreferenceKey.QUERY_PERIOD.toString(), "60");
        verify(sharedPreferences, times(1)).getString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "0");

        assertThat(timerTask.getPeriod(), is(60));
        assertThat(timerTask.getBackgroundPeriod(), is(0));
    }

    @Test
    public void testRun()
    {
        long actualSecond = System.currentTimeMillis() / 1000;

        timerTask.run();

        verify(dataHandler, times(1)).updateData(dataChannelsCaptor.capture());

        final Set<DataChannel> updateChannels = dataChannelsCaptor.getValue();
        assertThat(updateChannels.size(), is(2));
        assertThat(updateChannels, hasItems(DataChannel.STROKES, DataChannel.PARTICIPANTS));

        assertThat(timerTask.getLastUpdate(), is(actualSecond));
    }

    class TimerListener implements TimerTask.TimerUpdateListener
    {
        private String timerString;

        @Override
        public void onTimerUpdate(String timerString) {
            this.timerString = timerString;
        }

        public String getTimerString() {
            return timerString;
        }
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

        timerTask.onResume(true);

        assertThat(timerTask.getLastUpdate(), is(actualSecond));
        assertFalse(timerTask.isInBackgroundOperation());
    }

    @Test
    public void testOnPause() {
        timerTask.onResume(true);

        timerTask.onPause();

        assertTrue(timerTask.isInBackgroundOperation());
    }

    @Test
    public void testSetListener() {
        TimerListener timerListener = new TimerListener();

        timerTask.setListener(timerListener);

        timerTask.enable();

        assertThat(timerListener.getTimerString(), is("60/60s"));
    }
}

