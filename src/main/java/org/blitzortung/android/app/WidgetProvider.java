package org.blitzortung.android.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import org.blitzortung.android.app.view.AlertView;

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

        AlertView alertView = new AlertView(context);
        alertView.measure(150, 150);
        alertView.layout(0, 0, 150, 150);
        alertView.setDrawingCacheEnabled(true);
        Bitmap bitmap= alertView.getDrawingCache();

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        remoteViews.setImageViewBitmap(R.layout.widget, bitmap);

    }
}
