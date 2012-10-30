package org.blitzortung.android.alarm;

import org.blitzortung.android.app.R;

import android.content.res.Resources;
import android.widget.TextView;

public class AlarmLabel {

    private final TextView textView;

    private final Resources resources;

    public AlarmLabel(TextView textView, Resources resources) {
        this.textView = textView;
        this.resources = resources;
    }

    public void apply(AlarmStatus alarmStatus) {
        String warningText = "";

        if (alarmStatus != null) {
            int textColorResource = R.color.Green;

            AlarmResult result = alarmStatus.getCurrentActivity();

            if (result != null) {
                if (result.getClosestStrokeDistance() > 50) {
                    textColorResource = R.color.Green;
                } else if (result.getClosestStrokeDistance() > 20) {
                    textColorResource = R.color.Yellow;
                } else {
                    textColorResource = R.color.Red;
                }
                warningText = String.format("%.0f%s %s", result.getClosestStrokeDistance(), result.getDistanceUnitName(), result.getBearingName());
            }

            textView.setTextColor(resources.getColor(textColorResource));
        }

        textView.setText(warningText);
    }

}
