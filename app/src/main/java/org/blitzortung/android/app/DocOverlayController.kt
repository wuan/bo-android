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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.blitzortung.android.app.databinding.DocBalloonBinding
import org.blitzortung.android.app.databinding.DocOverlayBinding
import org.blitzortung.android.app.docoverlay.BalloonHorizontalAlignment
import org.blitzortung.android.app.docoverlay.BalloonRequest
import org.blitzortung.android.app.docoverlay.BalloonRow
import org.blitzortung.android.app.docoverlay.BalloonTailSide
import org.blitzortung.android.app.docoverlay.DocBalloonLayout
import org.blitzortung.android.app.docoverlay.DocBalloonViewBinder
import org.blitzortung.android.app.docoverlay.IntPoint
import org.blitzortung.android.app.docoverlay.IntRect
import org.blitzortung.android.preferences.PreferenceKey
import org.blitzortung.android.preferences.get

/**
 * One explanation shown as a balloon: the view it describes plus the text to display.
 *
 * [preferredTailSide] only expresses a preference; [DocBalloonLayout] picks the final side so
 * the tail still points at [view] even when the balloon has to move to avoid an overlap.
 * [horizontalAlignment] pins the balloon to an edge when needed, and [row] puts it into the
 * top or bottom group so the requested row structure is kept.
 */
data class DocTarget(
    val view: View,
    val text: CharSequence,
    val preferredTailSide: BalloonTailSide,
    val horizontalAlignment: BalloonHorizontalAlignment = BalloonHorizontalAlignment.CENTERED_ON_TARGET,
    val row: BalloonRow = BalloonRow.FLOATING,
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

        applySystemBarInsets(binding)

        // The balloons are laid out as soon as the overlay has a usable size. Re-running the
        // layout on every size change keeps them correct when the insets or orientation change
        // while the overlay is open.
        val container = binding.docBalloonContainer
        container.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val sizeChanged = right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop
            if (sizeChanged) {
                container.post {
                    if (overlayBinding === binding) {
                        layoutBalloons(binding)
                    }
                }
            }
        }
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
     * Keeps the balloons and the "Got it!" button clear of the system bars and display cutout,
     * while the scrim still covers the whole window.
     *
     * The insets become padding on the overlay root, so [DocOverlayBinding.docBalloonContainer]
     * shrinks to the usable area and serves as both the layout canvas and the coordinate origin.
     * Padding is only re-applied when it actually changes, so repeated inset dispatches do not
     * trigger an endless layout loop.
     */
    private fun applySystemBarInsets(binding: DocOverlayBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val paddingChanged = view.paddingLeft != bars.left ||
                view.paddingTop != bars.top ||
                view.paddingRight != bars.right ||
                view.paddingBottom != bars.bottom
            if (paddingChanged) {
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    /**
     * Creates the balloon views, measures them and places them according to [DocBalloonLayout].
     */
    private fun layoutBalloons(overlayBinding: DocOverlayBinding) {
        val container = overlayBinding.docBalloonContainer
        if (container.width <= 0 || container.height <= 0) {
            return
        }
        container.removeAllViews()

        val targets = targetsProvider().filter { it.view.isAttachedToWindow && it.view.isShown }
        if (targets.isEmpty()) {
            return
        }

        val containerLocation = screenLocationOf(container)
        val rowArea = rowLayoutArea(targets, containerLocation, container)
        val rowOrigin = IntPoint(containerLocation.x, containerLocation.y + rowArea.top)

        val requests = ArrayList<BalloonRequest>(targets.size)
        val bindings = ArrayList<DocBalloonBinding>(targets.size)
        targets.forEachIndexed { index, target ->
            val balloon = viewBinder.create(container, target.text)
            viewBinder.measure(balloon, rowArea.width - 2 * EDGE_MARGIN_PX)
            bindings += balloon
            requests += BalloonRequest(
                id = index,
                targetCenter = centerOf(target.view, rowOrigin),
                preferredSize = IntPoint(balloon.root.measuredWidth, balloon.root.measuredHeight),
                preferredTailSide = target.preferredTailSide,
                horizontalAlignment = target.horizontalAlignment,
                row = target.row,
                order = index,
            )
        }

        val layout = DocBalloonLayout(
            containerWidth = rowArea.width,
            containerHeight = rowArea.height,
            gap = GAP_PX,
            tailLength = DocBalloonViewBinder.TAIL_LENGTH_PX,
            edgeMargin = EDGE_MARGIN_PX,
        )
        val placed = layout.place(
            requests,
            listOfNotNull(boundsOf(overlayBinding.docButtonGotIt, rowOrigin)),
        )

        bindings.forEachIndexed { index, balloon ->
            placed.firstOrNull { it.id == index }?.let {
                viewBinder.apply(balloon, it.translated(rowArea.left, rowArea.top))
            }
        }
    }

    /**
     * The band the balloon rows may occupy, in container coordinates.
     *
     * The top row is anchored just below the highest documented view (the status row) and the
     * bottom stack just above the lowest one (the time slider), so the balloons sit next to the
     * controls they describe instead of being pinned to the overlay edges. The band is derived
     * from the targets, so it follows whatever is currently visible.
     */
    private fun rowLayoutArea(targets: List<DocTarget>, origin: IntPoint, container: View): IntRect {
        val fullArea = IntRect(0, 0, container.width, container.height)
        if (targets.any { it.row == BalloonRow.FLOATING }) {
            return fullArea
        }
        val top = targets.filter { it.row == BalloonRow.TOP }
            .mapNotNull { boundsOf(it.view, origin)?.bottom }
            .minOrNull() ?: fullArea.top
        val bottom = targets.filter { it.row == BalloonRow.BOTTOM }
            .mapNotNull { boundsOf(it.view, origin)?.top }
            .maxOrNull() ?: fullArea.bottom
        return if (bottom - top < MIN_ROW_AREA_PX) fullArea else IntRect(0, top, container.width, bottom)
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

        /** Minimum height of the band between the top and bottom controls for the row layout. */
        const val MIN_ROW_AREA_PX = 160
    }
}
