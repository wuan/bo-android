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

package org.blitzortung.android.app.docoverlay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import org.blitzortung.android.app.R
import org.blitzortung.android.app.databinding.DocBalloonBinding

/**
 * Inflates, measures and positions the balloon views used by the documentation overlay.
 *
 * The placement itself is computed by [DocBalloonLayout]; this class only translates the
 * resulting rectangles into view layout parameters and attaches the tail to the correct side.
 */
class DocBalloonViewBinder(private val layoutInflater: LayoutInflater) {
    /** Inflates a balloon for [text] and adds it to [container]. */
    fun create(container: ViewGroup, text: CharSequence): DocBalloonBinding {
        val binding = DocBalloonBinding.inflate(layoutInflater, container, false)
        binding.docBalloonText.text = text
        container.addView(binding.root)
        return binding
    }

    /** Measures the balloon so that its preferred size can be handed to [DocBalloonLayout]. */
    fun measure(binding: DocBalloonBinding, maxWidth: Int) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(maxWidth.coerceAtLeast(1), View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        binding.root.measure(widthSpec, heightSpec)
    }

    /**
     * Applies a placement result to the balloon.
     *
     * [placed] bounds describe the whole balloon including the tail strip; the body is inset on
     * the tail side by the tail length so that the outer edge stays exactly where the layout
     * put it.
     */
    fun apply(binding: DocBalloonBinding, placed: PlacedBalloon) {
        val body = binding.root
        val params = body.layoutParams as FrameLayout.LayoutParams
        val tail = tailLengthFor(placed.tailSide)
        val insetLeft = if (placed.tailSide == BalloonTailSide.LEFT) tail.x else 0
        val insetTop = if (placed.tailSide == BalloonTailSide.TOP) tail.y else 0
        val insetRight = if (placed.tailSide == BalloonTailSide.RIGHT) tail.x else 0
        val insetBottom = if (placed.tailSide == BalloonTailSide.BOTTOM) tail.y else 0
        params.leftMargin = placed.bounds.left + insetLeft
        params.topMargin = placed.bounds.top + insetTop
        params.width = (placed.bounds.width - insetLeft - insetRight).coerceAtLeast(1)
        params.height = (placed.bounds.height - insetTop - insetBottom).coerceAtLeast(1)
        body.layoutParams = params

        configureTail(
            binding.docBalloonTail,
            placed.tailSide,
            placed.tailTip.x - (placed.bounds.left + insetLeft),
            placed.tailTip.y - (placed.bounds.top + insetTop),
        )
    }

    private fun configureTail(tail: ImageView, side: BalloonTailSide, localTipX: Int, localTipY: Int) {
        val params = tail.layoutParams as RelativeLayout.LayoutParams
        params.removeRule(RelativeLayout.CENTER_VERTICAL)
        params.removeRule(RelativeLayout.CENTER_HORIZONTAL)
        params.leftMargin = 0
        params.topMargin = 0
        params.rightMargin = 0
        params.bottomMargin = 0
        tail.setImageResource(tailDrawable(side))

        when (side) {
            BalloonTailSide.TOP, BalloonTailSide.BOTTOM -> configureVerticalTail(params, side, localTipX)
            BalloonTailSide.LEFT, BalloonTailSide.RIGHT -> configureHorizontalTail(params, side, localTipY)
        }
        tail.layoutParams = params
    }

    private fun configureVerticalTail(params: RelativeLayout.LayoutParams, side: BalloonTailSide, localTipX: Int) {
        if (side == BalloonTailSide.TOP) {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            params.topMargin = -TAIL_LENGTH_PX
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            params.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            params.bottomMargin = -TAIL_LENGTH_PX
        }
        params.leftMargin = localTipX - TAIL_WIDTH_PX / 2
    }

    private fun configureHorizontalTail(params: RelativeLayout.LayoutParams, side: BalloonTailSide, localTipY: Int) {
        if (side == BalloonTailSide.LEFT) {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.leftMargin = -TAIL_LENGTH_PX
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.rightMargin = -TAIL_LENGTH_PX
        }
        params.topMargin = localTipY - TAIL_WIDTH_PX / 2
    }

    private fun tailDrawable(side: BalloonTailSide): Int = when (side) {
        BalloonTailSide.TOP -> R.drawable.doc_balloon_tail_top
        BalloonTailSide.BOTTOM -> R.drawable.doc_balloon_tail_bottom
        BalloonTailSide.LEFT -> R.drawable.doc_balloon_tail_left
        BalloonTailSide.RIGHT -> R.drawable.doc_balloon_tail_right
    }

    private fun tailLengthFor(side: BalloonTailSide): IntPoint = when (side) {
        BalloonTailSide.TOP, BalloonTailSide.BOTTOM -> IntPoint(TAIL_WIDTH_PX, TAIL_LENGTH_PX)
        BalloonTailSide.LEFT, BalloonTailSide.RIGHT -> IntPoint(TAIL_LENGTH_PX, TAIL_WIDTH_PX)
    }

    companion object {
        const val TAIL_LENGTH_PX = 26
        const val TAIL_WIDTH_PX = 22
    }
}
