package org.blitzortung.android.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.view.KeyEvent;
import android.view.View;
import org.blitzortung.android.app.R;

public class InfoDialog extends AlertDialog {

	public InfoDialog(Context context, PackageInfo pinfo) {
		super(context);
		
		setTitle(context.getResources().getText(R.string.app_name) + " V" + pinfo.versionName + " (" + pinfo.versionCode + ")");
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
}
