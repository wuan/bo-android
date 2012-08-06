package org.blitzortung.android.dialogs;

import org.blitzortung.android.app.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.KeyEvent;
import android.view.View;

public class InfoDialog extends AlertDialog {

	public InfoDialog(Context context) {
		super(context);
		
		PackageInfo pinfo;
		try {
			pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			throw new IllegalStateException(e);
		}
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
