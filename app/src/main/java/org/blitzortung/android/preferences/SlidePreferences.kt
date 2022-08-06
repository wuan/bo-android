/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.preferences

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView


class SlidePreferences(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs),
    SeekBar.OnSeekBarChangeListener {
    private val data: SliderData
    private lateinit var valueText: TextView
    private lateinit var slider: SeekBar

    init {
        data = SliderData(
            suffix = attrs.getAttributeValue(ATTRIBUTE_NAMESPACE, "text"),
            default = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "defaultValue", 30),
            minimum = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "min", 0),
            maximum = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "max", 80),
            step = attrs.getAttributeIntValue(null, "step", 1)
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialogView(): View {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(5, 5, 5, 5)

        val valueText = TextView(context)
        valueText.gravity = Gravity.CENTER_HORIZONTAL
        valueText.textSize = 32f
        //Set initial text
        valueText.text = data.text
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.addView(valueText, params)
        this.valueText = valueText

        slider = SeekBar(context).apply {
            setOnSeekBarChangeListener(this@SlidePreferences)
            layout.addView(this, params)
            max = data.size
            progress = data.offset
        }

        return layout
    }

    @Deprecated("Deprecated in Java")
    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        slider.max = data.size
        slider.progress = data.offset
    }

    @Deprecated("Deprecated in Java")
    override fun onSetInitialValue(should_restore: Boolean, defaultValue: Any?) {
        super.onSetInitialValue(should_restore, defaultValue)

        if (should_restore && shouldPersist()) {
            data.value = getPersistedInt(data.default)
        } else if (defaultValue is Int) {
            data.value = defaultValue
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, seekBarValue: Int, fromTouch: Boolean) {
        if (!fromTouch) {
            return
        }

        data.offset = seekBarValue
        valueText.text = data.text
        callChangeListener(data.value)
    }


    @Deprecated("Deprecated in Java")
    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            if (shouldPersist())
                persistInt(data.value)
        } else {
            onSetInitialValue(true, data.default)
        }

        super.onDialogClosed(positiveResult)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
    }

    companion object {
        private const val ATTRIBUTE_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}

