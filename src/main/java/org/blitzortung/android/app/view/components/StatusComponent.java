package org.blitzortung.android.app.view.components;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.blitzortung.android.alert.AlertLabelHandler;
import org.blitzortung.android.alert.event.AlertResultEvent;
import org.blitzortung.android.alert.AlertLabel;
import org.blitzortung.android.alert.event.AlertEvent;
import org.blitzortung.android.app.R;
import org.blitzortung.android.protocol.Consumer;

public class StatusComponent implements AlertLabel {

    private TextView status;

    private TextView warning;

    private ProgressBar progressBar;

    private ImageView errorIndicator;

    private final AlertLabelHandler alertLabelHandler;

    public StatusComponent(Activity activity) {
        status = (TextView) activity.findViewById(R.id.status);

        warning = (TextView) activity.findViewById(R.id.warning);

        progressBar = (ProgressBar) activity.findViewById(R.id.progress);
        progressBar.setVisibility(View.INVISIBLE);

        errorIndicator = (ImageView) activity.findViewById(R.id.error_indicator);
        errorIndicator.setVisibility(View.INVISIBLE);

        alertLabelHandler = new AlertLabelHandler(this, activity.getResources());
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

    private final Consumer<AlertEvent> alertEventConsumer = new Consumer<AlertEvent>() {
        @Override
        public void consume(AlertEvent event) {
            alertLabelHandler.apply(
                    event instanceof AlertResultEvent
                            ? ((AlertResultEvent) event).getAlertResult()
                            : null);
        }
    };

    public Consumer<AlertEvent> getAlertEventConsumer() {
        return alertEventConsumer;
    }
}
