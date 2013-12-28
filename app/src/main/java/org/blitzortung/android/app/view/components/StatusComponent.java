package org.blitzortung.android.app.view.components;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.blitzortung.android.alarm.AlarmLabel;
import org.blitzortung.android.app.R;

public class StatusComponent implements AlarmLabel {

    private TextView status;

    private TextView warning;

    private ProgressBar progressBar;

    private ImageView errorIndicator;

    public StatusComponent(Activity activity)
    {
        status = (TextView) activity.findViewById(R.id.status);

        warning = (TextView) activity.findViewById(R.id.warning);

        progressBar = (ProgressBar) activity.findViewById(R.id.progress);
        progressBar.setVisibility(View.INVISIBLE);

        errorIndicator = (ImageView) activity.findViewById(R.id.error_indicator);
        errorIndicator.setVisibility(View.INVISIBLE);
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
}
