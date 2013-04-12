package org.blitzortung.android.alarm;

import android.content.res.Resources;
import org.blitzortung.android.app.R;

public class AlarmLabelHandler {

    private final AlarmLabel alarmLabel;

    private final Resources resources;

    public AlarmLabelHandler(AlarmLabel alarmLabel, Resources resources) {
        this.alarmLabel = alarmLabel;
        this.resources = resources;
    }

    public void apply(AlarmResult alarmResult) {
        String warningText = "";

        int textColorResource = R.color.Green;

        if (alarmResult != null) {
            if (alarmResult.getClosestStrokeDistance() > 50) {
                textColorResource = R.color.Green;
            } else if (alarmResult.getClosestStrokeDistance() > 20) {
                textColorResource = R.color.Yellow;
            } else {
                textColorResource = R.color.Red;
            }
            warningText = String.format("%.0f%s %s", alarmResult.getClosestStrokeDistance(), alarmResult.getDistanceUnitName(), alarmResult.getBearingName());
        }

        alarmLabel.setAlarmTextColor(resources.getColor(textColorResource));
        alarmLabel.setAlarmText(warningText);
    }

}
