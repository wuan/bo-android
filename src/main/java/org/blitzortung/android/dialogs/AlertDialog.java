package org.blitzortung.android.dialogs;

import android.view.KeyEvent;
import android.view.View;
import org.blitzortung.android.app.AppService;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.view.AlertView;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class AlertDialog extends android.app.AlertDialog {

    private final ColorHandler colorHandler;
    private final AlertView alertView;
    private final AppService service;

    public AlertDialog(Main context, AppService service, ColorHandler colorHandler, int intervalDuration) {
        super(context);
        this.colorHandler = colorHandler;
        this.service = service;

        setTitle(context.getString(R.string.alarms));

        View dialog = getLayoutInflater().inflate(R.layout.alarm_dialog, null);

        alertView = (AlertView) dialog.findViewById(R.id.alarm_diagram);
        alertView.setColorHandler(colorHandler, intervalDuration);
        if (service != null) {
            alertView.getAlertEventConsumer().consume(service.getAlertEvent());
        }

        setView(dialog);
    }

    @Override
    public void onStart() {
        super.onStart();

        service.addAlertConsumer(alertView.getAlertEventConsumer());
        colorHandler.updateTarget();
    }

    @Override
    protected void onStop() {
        super.onStop();

        service.removeAlertListener(alertView.getAlertEventConsumer());
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
