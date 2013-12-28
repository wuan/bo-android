package org.blitzortung.android.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.map.color.ColorHandler;

public class AlarmDialog extends AlertDialog {

    private final ColorHandler colorHandler;

    public AlarmDialog(Context context, AlarmManager alarmManager, ColorHandler colorHandler, int intervalDuration) {
		super(context);
        this.colorHandler = colorHandler;

        setTitle(context.getString(R.string.alarms));

		View dialog = getLayoutInflater().inflate(R.layout.alarm_dialog, null);

		AlarmView alarmView = (AlarmView) dialog.findViewById(R.id.alarm_diagram);
		alarmView.setAlarmManager(alarmManager);
		alarmView.setColorHandler(colorHandler, intervalDuration);
		
		setView(dialog);
	}

    @Override
    public void onStart() {
        colorHandler.updateTarget();
    }

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			dismiss();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

}
