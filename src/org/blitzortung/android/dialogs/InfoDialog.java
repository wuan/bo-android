package org.blitzortung.android.dialogs;

import org.blitzortung.android.app.R;
import org.blitzortung.android.app.R.layout;
import org.blitzortung.android.app.R.string;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

public class InfoDialog extends AlertDialog {

	public InfoDialog(Context context) {
		super(context);
		setTitle(context.getString(R.string.app_name) + " V" + context.getString(R.string.app_version));
		View menu = getLayoutInflater().inflate(R.layout.info_dialog, null);
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
