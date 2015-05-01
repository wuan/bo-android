package org.blitzortung.android.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import org.blitzortung.android.app.R;

public class SettingsDialog extends AlertDialog {

	public SettingsDialog(Context context) {
		super(context);
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_dialog);

        setTitle(getContext().getResources().getText(R.string.preferences));

        ListView listView = (ListView)findViewById(R.id.settings_list);
        TextView child = new TextView(getContext());
        child.setText("asdf");
        listView.addView(child);
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
