package org.blitzortung.android.alert;

import android.content.res.Resources;
import org.blitzortung.android.app.R;

public class AlertLabelHandler {

    private final AlertLabel alertLabel;

    private final Resources resources;

    public AlertLabelHandler(AlertLabel alertLabel, Resources resources) {
        this.alertLabel = alertLabel;
        this.resources = resources;
    }

    public void apply(AlertResult alertResult) {
        String warningText = "";

        int textColorResource = R.color.Green;

        if (alertResult != null) {
            if (alertResult.getClosestStrokeDistance() > 50) {
                textColorResource = R.color.Green;
            } else if (alertResult.getClosestStrokeDistance() > 20) {
                textColorResource = R.color.Yellow;
            } else {
                textColorResource = R.color.Red;
            }
            warningText = String.format("%.0f%s %s", alertResult.getClosestStrokeDistance(), alertResult.getDistanceUnitName(), alertResult.getBearingName());
        }
        int color = resources.getColor(textColorResource);
        alertLabel.setAlarmTextColor(color);
        alertLabel.setAlarmText(warningText);
    }

}
