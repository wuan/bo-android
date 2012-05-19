package org.blitzortung.android.dialogs;

import org.blitzortung.android.app.R;
import org.blitzortung.android.app.R.layout;
import org.blitzortung.android.app.R.plurals;
import org.blitzortung.android.app.R.string;
import org.blitzortung.android.map.overlay.StrokesOverlay;

import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LegendDialog extends AlertDialog {

	public LegendDialog(Context context, StrokesOverlay strokesOverlay) {
		super(context);
		View menu = getLayoutInflater().inflate(R.layout.legend_dialog, null);

		setTitle(context.getString(R.string.legend));

		LinearLayout ll = (LinearLayout) menu;

		int colors[] = strokesOverlay.getColorHandler().getColors();
		int minutesPerColor = strokesOverlay.getMinutesPerColor();
		int totalMinutes = colors.length * minutesPerColor;

		TextView label = new TextView(context.getApplicationContext());
		label.setText(context.getString(R.string.total_time) + " "
				+ context.getResources().getQuantityString(R.plurals.minute, totalMinutes, totalMinutes) + ":");
		ll.addView(label);
		
		ll.addView(new TextView(context.getApplicationContext()));

		int endMinute = minutesPerColor;
		for (int color : colors) {
			TextView textView = new TextView(context.getApplicationContext());
			textView.setText(String.format(
					  context.getString(R.string.activity) + " "
					+ context.getString(R.string.during) + " "
					+ context.getResources().getQuantityString(R.plurals.minute, endMinute, endMinute)));
			textView.setTextColor(color);
			ll.addView(textView);
			endMinute += minutesPerColor;
		}

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
