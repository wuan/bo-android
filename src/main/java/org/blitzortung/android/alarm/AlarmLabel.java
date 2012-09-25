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

			int alarmSector = alarmStatus.getSectorWithClosestStroke();
			if (alarmSector >= 0) {
				AlarmResult result = alarmStatus.currentActivity();

				if (result != null) {
					if (result.getRange() > 3) {
						textColorResource = R.color.Green;
					} else if (result.getRange() > 1) {
						textColorResource = R.color.Yellow;
					} else {
						textColorResource = R.color.Red;
					}
					warningText = String.format("%.0fkm %s", result.getDistance() / 1000.0, alarmStatus.getSectorLabel(result.getSector()));
				}
			}
			textView.setTextColor(resources.getColor(textColorResource));
		}

		textView.setText(warningText);
	}

}
