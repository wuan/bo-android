package org.blitzortung.android.dialogs;

import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.map.overlay.color.ColorHandler;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

public class AlarmDialog extends AlertDialog {

	public AlarmDialog(Context context, AlarmManager alarmManager, ColorHandler colorHandler, int minutesPerColor) {
		super(context);
		
		setTitle(context.getString(R.string.alarms));

		View menu = getLayoutInflater().inflate(R.layout.alarm_dialog, null);

		AlarmView alarmView = (AlarmView) menu.findViewById(R.id.alarm_diagram);
		alarmView.setAlarmManager(alarmManager);
		alarmView.setColorHandler(colorHandler, minutesPerColor);
		
		setView(menu);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			dismiss();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

};
