package org.blitzortung.android.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.util.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPreferenceManager;

import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class DataServiceTest {

    @Mock
    private Handler handler;

    @Mock
    private Period period;

    @Mock
    private DataHandler dataHandler;

    @Captor
    ArgumentCaptor<Set<DataChannel>> dataChannelsCaptor;

    private SharedPreferences sharedPreferences;

    private DataService dataService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application);

        sharedPreferences.edit()
                .putString(PreferenceKey.QUERY_PERIOD.toString(), "60")
                .putString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "0")
                .putBoolean(PreferenceKey.SHOW_PARTICIPANTS.toString(), true)
                .putBoolean(PreferenceKey.ALARM_ENABLED.toString(), true)
                .commit();

        when(dataHandler.getIntervalDuration()).thenReturn(60);

        dataService = new DataService(handler, period);
        dataService.setDataHandler(dataHandler);

        dataService.onCreate();
    }

    @Test
    public void testOnBind() {
        Intent intent = mock(Intent.class);
        final IBinder binder = dataService.onBind(intent);

        assertThat(binder, is(instanceOf(DataService.DataServiceBinder.class)));

        assertThat(((DataService.DataServiceBinder) binder).getService(), is(sameInstance(dataService)));
    }

    @Test
    public void testInitialization() {
        verify(handler, times(0)).removeCallbacks(any(Runnable.class));
        verify(handler, times(0)).post(dataService);

        assertThat(dataService.getPeriod(), is(60));
        assertThat(dataService.getBackgroundPeriod(), is(0));
        assertFalse(dataService.isEnabled());
    }

    @Test
    public void testRun() {
        long actualSecond = System.currentTimeMillis() / 1000;

        dataService.run();

        verify(dataHandler, times(1)).updateData(dataChannelsCaptor.capture());
        verify(handler, times(1)).postDelayed(dataService, 1000);

        final Set<DataChannel> updateChannels = dataChannelsCaptor.getValue();
        assertThat(updateChannels.size(), is(2));
        assertThat(updateChannels, hasItems(DataChannel.STROKES, DataChannel.PARTICIPANTS));

        assertThat(dataService.getLastUpdate(), is(actualSecond));
    }

    class TimerListener implements DataService.DataServiceStatusListener {
        private String dataServiceStatus;

        @Override
        public void onDataServiceStatusUpdate(String dataServiceStatus) {
            this.dataServiceStatus = dataServiceStatus;
        }

        public String getDataServiceStatus() {
            return dataServiceStatus;
        }
    }

    @Test
    public void testRestart() {
        dataService.run();

        dataService.restart();

        assertThat(dataService.getLastUpdate(), is(0l));
    }

    @Test
    public void testOnResumeInRealtime() {
        when(dataHandler.isRealtime()).thenReturn(true);

        dataService.onResume();

        InOrder order = inOrder(handler);

        order.verify(handler).removeCallbacks(dataService);
        order.verify(handler).post(dataService);

        assertTrue(dataService.isEnabled());

        assertFalse(dataService.isInBackgroundOperation());
    }

    @Test
    public void testOnResumeInHistoricalDataMode() {
        when(dataHandler.isRealtime()).thenReturn(false);

        dataService.onResume();

        verify(handler, times(0)).removeCallbacks(dataService);
        verify(handler, times(0)).post(dataService);

        assertFalse(dataService.isEnabled());

        assertFalse(dataService.isInBackgroundOperation());
    }

    @Test
    public void testOnPause() {
        when(dataHandler.isRealtime()).thenReturn(true);

        dataService.onResume();

        assertTrue(dataService.onPause());

        assertTrue(dataService.isInBackgroundOperation());
    }

    @Test
    public void testOnPauseWithAlarmAndBackgroundPeriodEnabled() {
        sharedPreferences.edit()
                .putString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "60")
                .commit();
        dataService.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.BACKGROUND_QUERY_PERIOD.toString());

        when(dataHandler.isRealtime()).thenReturn(true);

        dataService.onResume();

        assertFalse(dataService.onPause());

        assertTrue(dataService.isInBackgroundOperation());
    }

    @Test
    public void testSetListener() {
        TimerListener timerListener = new TimerListener();

        dataService.setListener(timerListener);

        dataService.run();

        assertThat(timerListener.getDataServiceStatus(), is("60/60s"));
    }

    @Test
    public void testEnable() {
        dataService.enable();

        InOrder order = inOrder(handler);

        order.verify(handler).removeCallbacks(dataService);
        order.verify(handler).post(dataService);

        assertTrue(dataService.isEnabled());
    }
}
