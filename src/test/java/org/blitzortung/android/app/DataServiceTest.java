package org.blitzortung.android.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class DataServiceTest {

    @Implements(PreferenceManager.class)
    public static class ShadowPreferenceManager {
        private static SharedPreferences preferences = mock(SharedPreferences.class);

        @SuppressWarnings("UnusedDeclaration")
        @Implementation
        public static SharedPreferences getDefaultSharedPreferences(Context context) {
            return preferences;
        }
    }

    @Mock
    private Handler handler;

    @Mock
    private DataHandler dataHandler;

    @Captor
    ArgumentCaptor<Set<DataChannel>> dataChannelsCaptor;

    private SharedPreferences sharedPreferences;

    private DataService dataService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Robolectric.bindShadowClass(ShadowPreferenceManager.class);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Robolectric.application);
        reset(sharedPreferences);
        
        when(sharedPreferences.getString(PreferenceKey.QUERY_PERIOD.toString(), "60")).thenReturn("60");
        when(sharedPreferences.getString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "0")).thenReturn("0");
        when(sharedPreferences.getBoolean(PreferenceKey.SHOW_PARTICIPANTS.toString(), true)).thenReturn(true);
        when(sharedPreferences.getBoolean(PreferenceKey.ALARM_ENABLED.toString(), false)).thenReturn(true);

        when(dataHandler.getIntervalDuration()).thenReturn(60);
        
        dataService = new DataService(handler);
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
    public void testInitialization()
    {
        verify(handler, times(0)).removeCallbacks(any(Runnable.class));
        verify(handler, times(0)).post(dataService);
        verify(sharedPreferences, times(1)).registerOnSharedPreferenceChangeListener(any(DataService.class));
        verify(sharedPreferences, times(1)).getString(PreferenceKey.QUERY_PERIOD.toString(), "60");
        verify(sharedPreferences, times(1)).getString(PreferenceKey.BACKGROUND_QUERY_PERIOD.toString(), "0");
        verify(sharedPreferences, times(1)).getBoolean(PreferenceKey.SHOW_PARTICIPANTS.toString(), true);
        verify(sharedPreferences, times(1)).getBoolean(PreferenceKey.ALARM_ENABLED.toString(), false);
        
        assertThat(dataService.getPeriod(), is(60));
        assertThat(dataService.getBackgroundPeriod(), is(0));
        assertFalse(dataService.isEnabled());
    }

    @Test
    public void testRun()
    {        
        long actualSecond = System.currentTimeMillis() / 1000;

        dataService.run();

        verify(dataHandler, times(1)).updateData(dataChannelsCaptor.capture());
        verify(handler, times(1)).postDelayed(dataService, 1000);

        final Set<DataChannel> updateChannels = dataChannelsCaptor.getValue();
        assertThat(updateChannels.size(), is(2));
        assertThat(updateChannels, hasItems(DataChannel.STROKES, DataChannel.PARTICIPANTS));

        assertThat(dataService.getLastUpdate(), is(actualSecond));
    }

    class TimerListener implements DataService.DataServiceStatusListener
    {
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
        assertThat(dataService.getLastParticipantsUpdate(), is(0l));
    }

    @Test
    public void testOnResumeInRealtime  () {
        when(dataHandler.isRealtime()).thenReturn(true);

        dataService.onResume();

        InOrder order = inOrder(handler);

        order.verify(handler).removeCallbacks(dataService);
        order.verify(handler).post(dataService);

        assertTrue(dataService.isEnabled());
        
        assertFalse(dataService.isInBackgroundOperation());
    }
    
    @Test
    public void testOnResumeInHistoricalDataMode  () {
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

        dataService.onPause();

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
