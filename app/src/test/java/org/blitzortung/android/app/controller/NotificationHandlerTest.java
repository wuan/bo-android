package org.blitzortung.android.app.controller;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class NotificationHandlerTest {

    @Implements(Notification.class)
    public class ShadowNotification {
        @RealObject
        private Notification realNotification;

        public void __constructor__(int icon, CharSequence tickerText, long when) {
            realNotification.icon = icon;
            realNotification.tickerText = tickerText;
            realNotification.when = when;
        }
    }

    @Mock
    private Activity activity;

    @Mock
    private Resources resources;

    @Mock
    private NotificationManager notificationManager;

    private NotificationHandler notificationHandler;
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(activity.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager);
        when(activity.getResources()).thenReturn(resources);

        notificationHandler = new NotificationHandler(activity);
    }

    @Test
    public void testSendNotification() throws Exception {
        notificationHandler.sendNotification("foo");

        verify(notificationManager, times(1)).notify(anyInt(), any(Notification.class));
    }

    @Test
    public void testSendNotificationWithNullNotification() {
        when(activity.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(null);
        notificationHandler = new NotificationHandler(activity);

        notificationHandler.sendNotification("bar");
    }

    @Test
    public void testClearNotification() throws Exception {
        notificationHandler.clearNotification();

        verify(notificationManager, times(1)).cancel(anyInt());
    }

    @Test
    public void testClearNotificationWithNullNotification() {
        when(activity.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(null);
        notificationHandler = new NotificationHandler(activity);

        notificationHandler.clearNotification();
    }

}
