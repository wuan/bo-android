package org.blitzortung.android.dialogs;

import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import org.blitzortung.android.app.AppService;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.view.AlarmView;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class AlarmDialog extends AlertDialog {

    private final ColorHandler colorHandler;
    private final AlarmView alarmView;
    private final AppService service;

    public AlarmDialog(Main context, AppService service, ColorHandler colorHandler, int intervalDuration) {
        super(context);
        this.colorHandler = colorHandler;
        this.service = service;

        setTitle(context.getString(R.string.alarms));

        View dialog = getLayoutInflater().inflate(R.layout.alarm_dialog, null);

        alarmView = (AlarmView) dialog.findViewById(R.id.alarm_diagram);
        alarmView.setColorHandler(colorHandler, intervalDuration);
        if (service != null) {
            alarmView.onEvent(service.getAlertEvent());
        }

        setView(dialog);
    }

    @Override
    public void onStart() {
        super.onStart();

        service.addAlertListener(alarmView);
        colorHandler.updateTarget();
    }

    @Override
    protected void onStop() {
        super.onStop();

        service.removeAlertListener(alarmView);
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
