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

package org.blitzortung.android.app.controller

import android.view.View
import android.widget.RelativeLayout
import org.blitzortung.android.app.helper.ViewHelper.pxFromSp

class ButtonColumnHandler<V : View, G : Enum<G>>(private val buttonSize: Float) {

    data class GroupedView<V, G>(val view: V, val groups: Set<G>)

    private val elements: MutableList<GroupedView<V, G>>

    init {
        elements = arrayListOf()
    }

    fun addElement(element: V, vararg groups: G) {
        elements.add(GroupedView(element, groups.toSet()))
    }

    fun addAllElements(elements: Collection<V>, vararg groups: G) {
        this.elements.addAll(elements.map { GroupedView(it, groups.toSet()) })
    }

    fun updateButtonColumn() {
        var previousIndex = -1
        for (currentIndex in elements.indices) {
            val element = elements[currentIndex]
            val view = element.view
            if (view.visibility == View.VISIBLE) {
                val lp = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
                )
                lp.width = pxFromSp(view.context, buttonSize).toInt()
                lp.height = pxFromSp(view.context, buttonSize).toInt()
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
