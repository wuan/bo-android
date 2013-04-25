package org.blitzortung.android.app;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.blitzortung.android.data.DataHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class DataServiceTest {

    @Mock
    private Handler handler;

    @Mock
    private DataHandler dataHandler;

    private DataService dataService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        dataService = new DataService(handler);
        dataService.setDataHandler(dataHandler);
    }

    @Test
    public void testOnBind() {
        Intent intent = mock(Intent.class);
        final IBinder binder = dataService.onBind(intent);
        
        assertThat(binder, is(instanceOf(DataService.DataServiceBinder.class)));
                
        assertThat(((DataService.DataServiceBinder) binder).getService(), is(sameInstance(dataService)));
    }
}
