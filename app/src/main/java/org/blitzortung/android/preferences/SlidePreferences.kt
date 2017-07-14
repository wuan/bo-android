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
    private var minimumValue: Int

    init {
        unitSuffix = " " + attrs.getAttributeValue(ATTRIBUTE_NAMESPACE, "text")
        defaultValue = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "defaultValue", 30)
        minimumValue = attrs.getAttributeIntValue(null, "min", 0)

        //If there is a minimum value, we always add the minimum-value to the slider
        //E. g. max of 400 with min 20 value. if we were at 400, we would get 400 + 20
        //So always set the max-value of the Seekbar to maxValue - minValue
        maximumValue = attrs.getAttributeIntValue(ATTRIBUTE_NAMESPACE, "max", 80) - minimumValue
    }

    override fun onCreateDialogView(): View {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(5, 5, 5, 5)

        val valueText = TextView(context)
        valueText.gravity = Gravity.CENTER_HORIZONTAL
        valueText.textSize = 32f
        //Set initial text
        valueText.text =  "$currentValue $unitSuffix"
        val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        layout.addView(valueText, params)
        this.valueText = valueText

        slider = SeekBar(context).apply {
            setOnSeekBarChangeListener(this@SlidePreferences)
            layout.addView(this, params)

            //We need to adapt the value of the SeekBar to our Minimum-Value
            max = maximumValue
            progress = currentValue - minimumValue
        }

        return layout
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)

        slider!!.max = maximumValue
        //We need to adapt the value of the SeekBar to our Minimum-Value
        slider!!.progress = currentValue - minimumValue
    }

    override fun onSetInitialValue(should_restore: Boolean, defaultValue: Any?) {
        super.onSetInitialValue(should_restore, defaultValue)

        if (should_restore && shouldPersist()) {
            currentValue = getPersistedInt(this.defaultValue)
        } else if (defaultValue is Int) {
            currentValue = defaultValue
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, seekBarValue: Int, fromTouch: Boolean) {
        //Ignore events that wasn't done by the user
        if(!fromTouch)
            return

        //We need to adapt the value of the SeekBar to our Minimum-Value
        currentValue = seekBarValue + minimumValue

        valueText!!.text = "$currentValue $unitSuffix"

        callChangeListener(currentValue)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if(positiveResult) {
            if (shouldPersist())
                persistInt(currentValue)
        } else {
            //Use pressed Cancel, so reset currentValue
            onSetInitialValue(true, defaultValue)
        }

        super.onDialogClosed(positiveResult)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
    }

    companion object {
        private val ATTRIBUTE_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}

