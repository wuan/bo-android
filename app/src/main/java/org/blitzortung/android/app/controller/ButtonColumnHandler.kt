package org.blitzortung.android.app.controller

import android.view.View
import android.widget.RelativeLayout

import org.blitzortung.android.app.helper.ViewHelper

import java.util.ArrayList

class ButtonColumnHandler<V : View> {

    private val elements: MutableList<V>

    init {
        elements = ArrayList<V>()
    }

    fun addElement(element: V) {
        elements.add(element)
    }

    fun addAllElements(elements: Collection<V>) {
        this.elements.addAll(elements)
    }

    fun lockButtonColumn() {
        enableButtonElements(false)
    }

    fun unlockButtonColumn() {
        enableButtonElements(true)
    }

    private fun enableButtonElements(enabled: Boolean) {
        for (element in elements) {
            element.isEnabled = enabled
        }
    }

    fun updateButtonColumn() {
        var previousIndex = -1
        for (currentIndex in elements.indices) {
            val element = elements[currentIndex]
            if (element.visibility == View.VISIBLE) {
                val lp = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                lp.width = ViewHelper.pxFromSp(element, 55f).toInt()
                lp.height = ViewHelper.pxFromSp(element, 55f).toInt()
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1)
                if (previousIndex < 0) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1)
                } else {
                    lp.addRule(RelativeLayout.BELOW, elements[previousIndex].id)
                }
                element.layoutParams = lp
                previousIndex = currentIndex
            }
        }
    }


}
