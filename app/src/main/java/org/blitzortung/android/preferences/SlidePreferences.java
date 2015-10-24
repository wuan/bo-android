package org.blitzortung.android.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class SlidePreferences extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String ATTRIBUTE_NAMESPACE = "http://schemas.android.com/apk/res/android";

    private final Context context;
    private TextView valueText;
    private SeekBar slider;

    private final String unitSuffix;

    private final int defaultValue;
    private final int maximumValue;
    private int currentValue;

    public SlidePreferences(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        unitSuffix = " " + attrs.getAttributeValue(ATTRIBUTE_NAMESPACE, "text");
        defaultValue = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "defaultValue", 30);
        maximumValue = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "max", 80);
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(5, 5, 5, 5);

        valueText = new TextView(context);
        valueText.setGravity(Gravity.CENTER_HORIZONTAL);
        valueText.setTextSize(32);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(valueText, params);

        slider = new SeekBar(context);
        slider.setOnSeekBarChangeListener(this);
        layout.addView(slider, params);

        if (shouldPersist()) {
            currentValue = getPersistedInt(defaultValue);
        }

        slider.setMax(maximumValue);
        slider.setProgress(currentValue);

        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        slider.setMax(maximumValue);
        slider.setProgress(currentValue);
    }

    @Override
    protected void onSetInitialValue(boolean should_restore, Object defaultValue) {
        super.onSetInitialValue(should_restore, defaultValue);

        if (should_restore) {
            currentValue = shouldPersist() ? getPersistedInt(this.defaultValue) : this.defaultValue;
        } else {
            currentValue = (Integer) defaultValue;
        }
    }

    public void onProgressChanged(SeekBar seekBar, int currentValue, boolean fromTouch) {
        String t = String.valueOf(currentValue);

        valueText.setText(unitSuffix == null ? t : t.concat(unitSuffix));

        if (shouldPersist()) {
            persistInt(currentValue);
        }

        callChangeListener(currentValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}

