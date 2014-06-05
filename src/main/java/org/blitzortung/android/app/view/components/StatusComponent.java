package org.blitzortung.android.app.view.components;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.blitzortung.android.AlertResultEvent;
import org.blitzortung.android.alarm.AlarmLabel;
import org.blitzortung.android.alarm.AlarmLabelHandler;
import org.blitzortung.android.alarm.AlertEvent;
import org.blitzortung.android.app.R;
import org.blitzortung.android.protocol.Event;
import org.blitzortung.android.protocol.Listener;

public class StatusComponent implements AlarmLabel, Listener {

    private TextView status;

    private TextView warning;

    private ProgressBar progressBar;

    private ImageView errorIndicator;

    private final AlarmLabelHandler alarmLabelHandler;

    public StatusComponent(Activity activity) {
        status = (TextView) activity.findViewById(R.id.status);

        warning = (TextView) activity.findViewById(R.id.warning);

        progressBar = (ProgressBar) activity.findViewById(R.id.progress);
        progressBar.setVisibility(View.INVISIBLE);

        errorIndicator = (ImageView) activity.findViewById(R.id.error_indicator);
        errorIndicator.setVisibility(View.INVISIBLE);

        alarmLabelHandler = new AlarmLabelHandler(this, activity.getResources());
    }

    public void startProgress() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
    }

    public void stopProgress() {
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setProgress(progressBar.getMax());
    }

    public void indicateError(boolean indicateError) {
        errorIndicator.setVisibility(indicateError ? View.VISIBLE : View.INVISIBLE);
    }

    public void setText(String statusText) {
        status.setText(statusText);
    }

    @Override
    public void setAlarmTextColor(int color) {
        warning.setTextColor(color);
    }

    @Override
    public void setAlarmText(String alarmText) {
        warning.setText(alarmText);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof AlertEvent) {
            alarmLabelHandler.apply(
                    event instanceof AlertResultEvent
                            ? ((AlertResultEvent) event).getAlertResult()
                            : null);
        }
    }
}
