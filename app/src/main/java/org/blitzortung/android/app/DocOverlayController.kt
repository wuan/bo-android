/*

   Copyright 2026 Andreas Würl

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

package org.blitzortung.android.app

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.view.doOnLayout
import org.blitzortung.android.app.databinding.DocBalloonBinding
import org.blitzortung.android.app.databinding.DocOverlayBinding
import org.blitzortung.android.app.docoverlay.BalloonHorizontalAlignment
import org.blitzortung.android.app.docoverlay.BalloonRequest
import org.blitzortung.android.app.docoverlay.BalloonTailSide
import org.blitzortung.android.app.docoverlay.BalloonVerticalSlot
import org.blitzortung.android.app.docoverlay.DocBalloonLayout
import org.blitzortung.android.app.docoverlay.DocBalloonViewBinder
import org.blitzortung.android.app.docoverlay.IntPoint
import org.blitzortung.android.app.docoverlay.IntRect
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get

/**
 * One explanation shown as a balloon: the view it describes plus the text to display.
 *
 * [preferredTailSide] only expresses a preference; [DocBalloonLayout] picks the final side so
 * the tail still points at [view] even when the balloon has to move to avoid an overlap.
 * [horizontalAlignment] pins the balloon to an edge when needed (for right aligned controls),
 * and [verticalSlot] puts the balloon into the bottom stack so the bottom-to-top order is kept.
 */
data class DocTarget(
    val view: View,
    val text: CharSequence,
    val preferredTailSide: BalloonTailSide,
    val horizontalAlignment: BalloonHorizontalAlignment = BalloonHorizontalAlignment.CENTERED_ON_TARGET,
    val verticalSlot: BalloonVerticalSlot = BalloonVerticalSlot.NONE,
)

/**
 * Controls the quick documentation overlay that explains the main screen elements.
 *
 * Each explanation is rendered as a balloon whose tail points at the center of the view it
 * describes. The target centers are computed at runtime, and the balloons are packed by
 * [DocBalloonLayout] so that they never overlap each other or the "Got it!" button.
 *
 * The overlay is attached to [parent] and blocks the content behind it until the user
 * dismisses it. The dismissal is remembered via [PreferenceKey.DOC_OVERLAY_SHOWN] so the
 * overlay is only shown automatically once; afterwards it can be opened again on demand
 * from the main menu.
 */
class DocOverlayController(
    private val parent: ViewGroup,
    private val layoutInflater: LayoutInflater,
    private val preferences: SharedPreferences,
    private val targetsProvider: () -> List<DocTarget> = { emptyList() },
) {
    private val viewBinder = DocBalloonViewBinder(layoutInflater)
    private var overlayBinding: DocOverlayBinding? = null

    val isShowing: Boolean
        get() = overlayBinding != null

    fun showIfNotShownBefore() {
        if (!hasBeenShownBefore()) {
            show()
        }
    }

    fun show() {
        if (overlayBinding != null) {
            return
        }
        val binding = DocOverlayBinding.inflate(layoutInflater, parent, false)
        binding.docButtonGotIt.setOnClickListener { dismissAndRemember() }
        parent.addView(binding.root)
        overlayBinding = binding

        // The balloons need the final overlay size, so wait until it has been laid out once.
        binding.root.doOnLayout { layoutBalloons(binding) }
    }

    fun dismiss() {
        val binding = overlayBinding ?: return
        parent.removeView(binding.root)
        overlayBinding = null
    }

    fun dismissAndRemember() {
        dismiss()
        preferences.edit { putBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, true) }
    }

    fun hasBeenShownBefore(): Boolean = preferences.get(PreferenceKey.DOC_OVERLAY_SHOWN, false)

    /**
     * Creates the balloon views, measures them and places them according to [DocBalloonLayout].
     */
    private fun layoutBalloons(overlayBinding: DocOverlayBinding) {
        val container = overlayBinding.docBalloonContainer
        val root = overlayBinding.root
        if (root.width <= 0 || root.height <= 0) {
            return
        }
        container.removeAllViews()

        val targets = targetsProvider().filter { it.view.isAttachedToWindow && it.view.isShown }
        if (targets.isEmpty()) {
            return
        }

        val rootLocation = screenLocationOf(root)
        val requests = ArrayList<BalloonRequest>(targets.size)
        val bindings = ArrayList<DocBalloonBinding>(targets.size)
        targets.forEachIndexed { index, target ->
            val balloon = viewBinder.create(container, target.text)
            viewBinder.measure(balloon, root.width - 2 * EDGE_MARGIN_PX)
            bindings += balloon
            requests += BalloonRequest(
                id = index,
                targetCenter = centerOf(target.view, rootLocation),
                preferredSize = IntPoint(balloon.root.measuredWidth, balloon.root.measuredHeight),
                preferredTailSide = target.preferredTailSide,
                horizontalAlignment = target.horizontalAlignment,
                verticalSlot = target.verticalSlot,
                order = index,
            )
        }

        val layout = DocBalloonLayout(
            containerWidth = root.width,
            containerHeight = root.height,
            gap = GAP_PX,
            tailLength = DocBalloonViewBinder.TAIL_LENGTH_PX,
            edgeMargin = EDGE_MARGIN_PX,
        )
        val placed = layout.place(requests, listOfNotNull(boundsOf(overlayBinding.docButtonGotIt, rootLocation)))

        bindings.forEachIndexed { index, balloon ->
            placed.firstOrNull { it.id == index }?.let { viewBinder.apply(balloon, it) }
        }
    }

    private fun screenLocationOf(view: View): IntPoint {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return IntPoint(location[0], location[1])
    }

    private fun centerOf(view: View, origin: IntPoint): IntPoint {
        val location = screenLocationOf(view)
        val width = if (view.width > 0) view.width else view.measuredWidth
        val height = if (view.height > 0) view.height else view.measuredHeight
        return IntPoint(
            location.x - origin.x + width / 2,
            location.y - origin.y + height / 2,
        )
    }

    private fun boundsOf(view: View, origin: IntPoint): IntRect? {
        if (view.width <= 0 || view.height <= 0) {
            return null
        }
        val location = screenLocationOf(view)
        val left = location.x - origin.x
        val top = location.y - origin.y
        return IntRect(left, top, left + view.width, top + view.height)
    }

    private companion object {
        const val GAP_PX = 8
        const val EDGE_MARGIN_PX = 8
    }
}
