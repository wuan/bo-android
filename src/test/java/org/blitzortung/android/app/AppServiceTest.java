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
public class AppServiceTest {

    @Mock
    private Handler handler;

    @Mock
    private Period period;

    @Mock
    private DataHandler dataHandler;

    @Captor
    ArgumentCaptor<Set<DataChannel>> dataChannelsCaptor;

    private SharedPreferences sharedPreferences;

    private AppService appService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application);

        sharedPreferences.edit()
                .putString(PreferenceKey.QUERY_PERIOD.toString(), "60")
                .putString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "0")
                .putBoolean(PreferenceKey.SHOW_PARTICIPANTS.toString(), true)
                .putBoolean(PreferenceKey.ALERT_ENABLED.toString(), true)
                .commit();

        when(dataHandler.getIntervalDuration()).thenReturn(60);

        appService = new AppService(handler, period);
        appService.setDataHandler(dataHandler);

        appService.onCreate();
    }

    @Test
    public void testOnBind() {
        Intent intent = mock(Intent.class);
        final IBinder binder = appService.onBind(intent);

        assertThat(binder, is(instanceOf(AppService.DataServiceBinder.class)));

        assertThat(((AppService.DataServiceBinder) binder).getService(), is(sameInstance(appService)));
    }

    @Test
    public void testInitialization() {
        verify(handler, times(0)).removeCallbacks(any(Runnable.class));
        verify(handler, times(0)).post(appService);

        assertThat(appService.getPeriod(), is(60));
        assertThat(appService.getBackgroundPeriod(), is(0));
        assertFalse(appService.isEnabled());
    }

    @Test
    public void testRun() {
        long actualSecond = System.currentTimeMillis() / 1000;

        appService.run();

        verify(dataHandler, times(1)).updateData(dataChannelsCaptor.capture());
        verify(handler, times(1)).postDelayed(appService, 1000);

        final Set<DataChannel> updateChannels = dataChannelsCaptor.getValue();
        assertThat(updateChannels.size(), is(2));
        assertThat(updateChannels, hasItems(DataChannel.STROKES, DataChannel.PARTICIPANTS));

        assertThat(appService.getLastUpdate(), is(actualSecond));
    }

    @Test
    public void testRestart() {
        appService.run();

        appService.restart();

        assertThat(appService.getLastUpdate(), is(0l));
    }

    @Test
    public void testOnResumeInRealtime() {
        when(dataHandler.isRealtime()).thenReturn(true);

        //appService.resumeDataService();

        InOrder order = inOrder(handler);

        order.verify(handler).removeCallbacks(appService);
        order.verify(handler).post(appService);

        assertTrue(appService.isEnabled());
    }

    @Test
    public void testOnResumeInHistoricalDataMode() {
        when(dataHandler.isRealtime()).thenReturn(false);

        //appService.resumeDataService();

        verify(handler, times(0)).removeCallbacks(appService);
        verify(handler, times(0)).post(appService);

        assertFalse(appService.isEnabled());
    }

    @Test
    public void testOnPause() {
        when(dataHandler.isRealtime()).thenReturn(true);

        //appService.resumeDataService();

        //assertTrue(appService.suspendDataService());
    }

    @Test
    public void testOnPauseWithAlarmAndBackgroundPeriodEnabled() {
        sharedPreferences.edit()
                .putString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "60")
                .commit();
        appService.onSharedPreferenceChanged(sharedPreferences, PreferenceKey.BACKGROUND_QUERY_PERIOD.toString());

        when(dataHandler.isRealtime()).thenReturn(true);

        //appService.resumeDataService();

        //assertFalse(appService.suspendDataService());
    }

    @Test
    public void testEnable() {
        //appService.enable();

        InOrder order = inOrder(handler);

        order.verify(handler).removeCallbacks(appService);
        order.verify(handler).post(appService);

        assertTrue(appService.isEnabled());
    }
}
