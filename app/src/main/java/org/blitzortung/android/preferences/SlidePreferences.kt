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


class SlidePreferences(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs), SeekBar.OnSeekBarChangeListener {
    private val unitSuffix: String
    private val defaultValue: Int
    private val maximumValue: Int
    private var valueText: TextView? = null
    private var slider: SeekBar? = null
    private var currentValue: Int = 0

    init {
        unitSuffix = " " + attrs.getAttributeValue(ATTRIBUTE_NAMESPACE, "text")
        defaultValue = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "defaultValue", 30)
        maximumValue = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "max", 80)
    }

    override fun onCreateDialogView(): View {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(5, 5, 5, 5)

        val valueText = TextView(context)
        valueText.gravity = Gravity.CENTER_HORIZONTAL
        valueText.textSize = 32f
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        layout.addView(valueText, params)
        this.valueText = valueText

        slider = SeekBar(context)
        slider!!.setOnSeekBarChangeListener(this)
        layout.addView(slider, params)

        if (shouldPersist()) {
            currentValue = getPersistedInt(defaultValue)
        }

        slider!!.max = maximumValue
        slider!!.progress = currentValue

        return layout
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)

        slider!!.max = maximumValue
        slider!!.progress = currentValue
    }

    override fun onSetInitialValue(should_restore: Boolean, defaultValue: Any?) {
        super.onSetInitialValue(should_restore, defaultValue)

        if (should_restore) {
            currentValue = if (shouldPersist()) getPersistedInt(this.defaultValue) else this.defaultValue
        } else if (defaultValue != null) {
            currentValue = defaultValue as Int
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, currentValue: Int, fromTouch: Boolean) {
        val t = currentValue.toString()

        valueText!!.text = t + unitSuffix

        if (shouldPersist()) {
            persistInt(currentValue)
        }

        callChangeListener(currentValue)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
    }

    companion object {
        private val ATTRIBUTE_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}

