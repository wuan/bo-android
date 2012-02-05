package org.blitzortung.android.app;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

public class InfoDialog extends AlertDialog {

	public InfoDialog(Context context) {
		super(context);
		setTitle("Blitzortung.org Viewer V0.1");
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
