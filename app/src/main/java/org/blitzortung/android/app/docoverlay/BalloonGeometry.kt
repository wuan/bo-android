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
     *
     * For edge aligned balloons the perpendicular coordinate is biased towards the matching
     * side (left aligned balloons get their tail near the left end, right aligned ones near the
     * right end), so the handle sits on the same side as the balloon.
     */
    fun tailTipFor(
        target: IntPoint,
        bounds: IntRect,
        side: BalloonTailSide,
        alignment: BalloonHorizontalAlignment = BalloonHorizontalAlignment.CENTERED_ON_TARGET,
    ): IntPoint = when (side) {
        BalloonTailSide.TOP -> IntPoint(clampTailX(target.x, bounds, alignment), bounds.top + tailLength)
        BalloonTailSide.BOTTOM -> IntPoint(clampTailX(target.x, bounds, alignment), bounds.bottom - tailLength)
        BalloonTailSide.LEFT -> IntPoint(bounds.left + tailLength, clampTailY(target.y, bounds))
        BalloonTailSide.RIGHT -> IntPoint(bounds.right - tailLength, clampTailY(target.y, bounds))
    }

    /**
     * Horizontal position of the tail along a top/bottom edge. The tail is indented from the
     * rounded corners by [EDGE_INDENT_FRACTION] of the balloon width, so edge aligned balloons
     * keep their handle clear of the corners while centered balloons still point at the target.
     */
    private fun clampTailX(x: Int, bounds: IntRect, alignment: BalloonHorizontalAlignment): Int {
        val indent = (bounds.width * EDGE_INDENT_FRACTION).toInt()
        val minX = bounds.left + indent
        val maxX = bounds.right - 1 - indent
        val anchor = when (alignment) {
            BalloonHorizontalAlignment.LEFT_EDGE -> minX
            BalloonHorizontalAlignment.RIGHT_EDGE -> maxX
            BalloonHorizontalAlignment.CENTERED_ON_TARGET -> x
        }
        return if (maxX < minX) bounds.centerX() else anchor.coerceIn(minX, maxX)
    }

    /** Vertical position of the tail along a left/right edge, indented from the rounded corners. */
    private fun clampTailY(y: Int, bounds: IntRect): Int {
        val indent = (bounds.height * EDGE_INDENT_FRACTION).toInt()
        val minY = bounds.top + indent
        val maxY = bounds.bottom - 1 - indent
        return if (maxY < minY) bounds.centerY() else y.coerceIn(minY, maxY)
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

        /** Fraction of the balloon size the tail is kept away from the rounded corners. */
        const val EDGE_INDENT_FRACTION = 0.05
    }
}
