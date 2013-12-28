package org.blitzortung.android.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import org.blitzortung.android.app.view.AlarmView;

public class WidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++)
        {
            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context,
                                 AppWidgetManager appWidgetManager,
                                 int appWidgetId) {

        AlarmView alarmView = new AlarmView(context);
        alarmView.measure(150, 150);
        alarmView.layout(0, 0, 150, 150);
        alarmView.setDrawingCacheEnabled(true);
        Bitmap bitmap=alarmView.getDrawingCache();

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        remoteViews.setImageViewBitmap(R.layout.widget, bitmap);

    }
}
