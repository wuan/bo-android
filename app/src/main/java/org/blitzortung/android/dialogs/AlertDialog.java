package org.blitzortung.android.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import org.blitzortung.android.app.AppService;
import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.view.AlertView;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class AlertDialog extends android.app.AlertDialog {

    private final ColorHandler colorHandler;
    private AlertView alertView;
    private final AppService service;
    int intervalDuration;

    public AlertDialog(Context context, AppService service, ColorHandler colorHandler) {
        super(context);
        this.colorHandler = colorHandler;
        this.service = service;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.alarm_dialog);
    }

    public void setIntervalDuration(int intervalDuration) {
        this.intervalDuration = intervalDuration;
    }

    @Override
    public void onStart() {
        super.onStart();

        alertView = (AlertView) findViewById(R.id.alarm_diagram);

        setTitle(getContext().getString(R.string.alarms));

        if (service != null) {
            alertView.setColorHandler(colorHandler, service.getDataHandler().getIntervalDuration());
            alertView.getAlertEventConsumer().consume(service.getAlertEvent());
            service.addAlertConsumer(alertView.getAlertEventConsumer());
        }
        colorHandler.updateTarget();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (service != null) {
            service.removeAlertListener(alertView.getAlertEventConsumer());
        }
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
