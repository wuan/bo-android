package org.blitzortung.android.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

import org.blitzortung.android.app.R;
import org.blitzortung.android.app.components.VersionComponent;

public class InfoDialog extends AlertDialog {

    public InfoDialog(Context context, VersionComponent versionComponent) {
        super(context);

        setTitle(context.getResources().getText(R.string.app_name) +
                " V" + versionComponent.getVersionName() +
                " (" + versionComponent.getVersionCode() + ")");
        @SuppressLint("InflateParams") View infoDialogView = getLayoutInflater().inflate(R.layout.info_dialog, null, true);
        setView(infoDialogView);
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
