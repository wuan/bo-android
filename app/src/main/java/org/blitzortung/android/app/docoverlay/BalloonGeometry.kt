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

/**
 * Pure geometry helpers shared by the balloon layout: tail side selection, tail tip computation
 * and the box size that includes the pointer. Kept free of Android dependencies so it can be
 * unit tested directly through [DocBalloonLayout].
 */
internal class BalloonGeometry(
    private val containerWidth: Int,
    private val containerHeight: Int,
    private val tailLength: Int,
    private val edgeMargin: Int,
) {
    /**
     * Grows the body size by the tail length on the tail side, so the placed rectangle always
     * covers the complete balloon including the pointer.
     */
    fun boxSize(size: IntPoint, side: BalloonTailSide): IntPoint = when (side) {
        BalloonTailSide.TOP, BalloonTailSide.BOTTOM -> IntPoint(size.x, size.y + tailLength)
        BalloonTailSide.LEFT, BalloonTailSide.RIGHT -> IntPoint(size.x + tailLength, size.y)
    }

    /**
     * The point on the balloon box that faces the target: the tip of the tail. The coordinate
     * along the tail axis is taken from the tail strip (so it is always inside the box), while
     * the perpendicular coordinate follows the target center and is clamped to the tail width.
     */
    fun tailTipFor(target: IntPoint, bounds: IntRect, side: BalloonTailSide): IntPoint = when (side) {
        BalloonTailSide.TOP -> IntPoint(
            target.x.coerceIn(bounds.left, bounds.right - 1),
            bounds.top + tailLength,
        )
        BalloonTailSide.BOTTOM -> IntPoint(
            target.x.coerceIn(bounds.left, bounds.right - 1),
            bounds.bottom - tailLength,
        )
        BalloonTailSide.LEFT -> IntPoint(
            bounds.left + tailLength,
            target.y.coerceIn(bounds.top, bounds.bottom - 1),
        )
        BalloonTailSide.RIGHT -> IntPoint(
            bounds.right - tailLength,
            target.y.coerceIn(bounds.top, bounds.bottom - 1),
        )
    }

    /**
     * Picks the tail side with room around the target, preferring the requested side when it
     * is still plausible for the target's position inside the container.
     */
    fun chooseTailSide(request: BalloonRequest, size: IntPoint): BalloonTailSide {
        val center = request.targetCenter
        val fitsBelow = center.y + tailLength + size.y + edgeMargin <= containerHeight
        val fitsAbove = center.y - tailLength - size.y - edgeMargin >= 0
        val fitsRight = center.x + tailLength + size.x + edgeMargin <= containerWidth
        val fitsLeft = center.x - tailLength - size.x - edgeMargin >= 0

        val candidate = when (request.preferredTailSide) {
            BalloonTailSide.TOP -> if (fitsBelow) BalloonTailSide.TOP else BalloonTailSide.BOTTOM
            BalloonTailSide.BOTTOM -> if (fitsAbove) BalloonTailSide.BOTTOM else BalloonTailSide.TOP
            BalloonTailSide.RIGHT -> if (fitsLeft) BalloonTailSide.RIGHT else BalloonTailSide.LEFT
            BalloonTailSide.LEFT -> if (fitsRight) BalloonTailSide.LEFT else BalloonTailSide.RIGHT
        }
        return if (canPlace(candidate, center, size)) candidate else fallbackSide(center, size)
    }

    private fun canPlace(side: BalloonTailSide, center: IntPoint, size: IntPoint): Boolean = when (side) {
        BalloonTailSide.TOP -> center.y + tailLength + size.y + edgeMargin <= containerHeight
        BalloonTailSide.BOTTOM -> center.y - tailLength - size.y - edgeMargin >= 0
        BalloonTailSide.RIGHT -> center.x - tailLength - size.x - edgeMargin >= 0
        BalloonTailSide.LEFT -> center.x + tailLength + size.x + edgeMargin <= containerWidth
    }

    private fun fallbackSide(center: IntPoint, size: IntPoint): BalloonTailSide =
        if (center.y + size.y <= containerHeight - center.y) BalloonTailSide.TOP else BalloonTailSide.BOTTOM

    /** Horizontal anchor for a balloon inside its row, honouring its [BalloonHorizontalAlignment]. */
    fun alignedLeft(request: BalloonRequest, boxWidth: Int): Int = when (request.horizontalAlignment) {
        BalloonHorizontalAlignment.LEFT_EDGE -> edgeMargin
        BalloonHorizontalAlignment.RIGHT_EDGE -> containerWidth - edgeMargin - boxWidth
        BalloonHorizontalAlignment.CENTERED_ON_TARGET -> request.targetCenter.x - boxWidth / 2
    }

    /** Clamps the body size so that the complete box (body plus tail) still fits the container. */
    fun clampSize(size: IntPoint): IntPoint = IntPoint(
        size.x.coerceIn(MIN_SIZE, (containerWidth - 2 * edgeMargin - tailLength).coerceAtLeast(MIN_SIZE)),
        size.y.coerceIn(MIN_SIZE, (containerHeight - 2 * edgeMargin - tailLength).coerceAtLeast(MIN_SIZE)),
    )

    /** Keeps a rectangle inside the container margins. */
    fun clampToContainer(rect: IntRect): IntRect {
        val maxLeft = (containerWidth - edgeMargin - rect.width).coerceAtLeast(edgeMargin)
        val maxTop = (containerHeight - edgeMargin - rect.height).coerceAtLeast(edgeMargin)
        val left = rect.left.coerceIn(edgeMargin, maxLeft)
        val top = rect.top.coerceIn(edgeMargin, maxTop)
        return IntRect(left, top, left + rect.width, top + rect.height)
    }

    private companion object {
        const val MIN_SIZE = 1
    }
}
