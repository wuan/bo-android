package org.blitzortung.android.alarm;

import android.content.res.Resources;
import android.widget.TextView;
import org.blitzortung.android.app.R;

public class AlarmLabelHandler {

    private final AlarmLabel alarmLabel;

    private final Resources resources;

    public AlarmLabelHandler(AlarmLabel alarmLabel, Resources resources) {
        this.alarmLabel = alarmLabel;
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

            alarmLabel.setAlarmTextColor(resources.getColor(textColorResource));
        }

        alarmLabel.setAlarmText(warningText);
    }

}
