package org.blitzortung.android.app.controller

import android.view.View
import android.widget.RelativeLayout
import org.blitzortung.android.app.BuildConfig

import org.blitzortung.android.app.helper.ViewHelper

import java.util.ArrayList

class ButtonColumnHandler<V : View, G : Enum<G>>(private val buttonSize: Float) {

    data class GroupedView<V, G>(val view: V, val groups: Set<G>) {
        constructor(view: V, vararg groups: G) : this(view, groups.toSet()) {}
    }

    private val elements: MutableList<GroupedView<V, G>>

    init {
        elements = arrayListOf()
    }

    fun addElement(element: V, vararg groups: G) {
        elements.add(GroupedView(element, *groups))
    }

    fun addAllElements(elements: Collection<V>, vararg groups: G) {
        this.elements.addAll(elements.map { GroupedView(it, *groups) })
    }

    fun lockButtonColumn(vararg groups: G) {
        enableButtonElements(false, *groups)
    }

    fun unlockButtonColumn(vararg groups: G) {
        enableButtonElements(true, *groups)
    }

    private fun enableButtonElements(enabled: Boolean, vararg groups: G) {

        val filter: (GroupedView<V, G>) -> Boolean = if (groups.isEmpty()) {
            element -> true
        } else {
            element -> element.groups.intersect(groups.toSet()).isNotEmpty()
        }

        elements.filter { filter.invoke(it) }.forEach {
            it.view.isEnabled = enabled
        }
    }

    fun updateButtonColumn() {
        var previousIndex = -1
        for (currentIndex in elements.indices) {
            val element = elements[currentIndex]
            val view = element.view
            if (view.visibility == View.VISIBLE) {
                val lp = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                lp.width = ViewHelper.pxFromSp(view, buttonSize).toInt()
                lp.height = ViewHelper.pxFromSp(view, buttonSize).toInt()
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1)
                if (previousIndex < 0) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1)
                } else {
                    lp.addRule(RelativeLayout.BELOW, elements[previousIndex].view.id)
                }
                view.layoutParams = lp
                previousIndex = currentIndex
            }
        }
    }


}
