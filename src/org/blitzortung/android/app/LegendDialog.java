package org.blitzortung.android.app;

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

		int endMinute = totalMinutes;
		for (int color : colors) {
			TextView textView = new TextView(context.getApplicationContext());
			textView.setText(String.format("\t" + context.getString(R.string.minute) + " %d - %d", endMinute - minutesPerColor, endMinute - 1));
			textView.setTextColor(color);
			ll.addView(textView);
			endMinute -= minutesPerColor;
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
