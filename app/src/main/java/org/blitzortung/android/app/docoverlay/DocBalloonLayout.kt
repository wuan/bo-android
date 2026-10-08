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
 * Side of a balloon the pointer/tail is attached to.
 *
 * The tail points from the balloon towards its target, so [TOP] means the balloon sits
 * below its target and the tail is drawn on the balloon's top edge.
 */
enum class BalloonTailSide {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT,
}

/** Immutable integer point, in the same coordinate space as the container. */
data class IntPoint(val x: Int, val y: Int)

/** Immutable integer rectangle whose right/bottom edges are exclusive. */
data class IntRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun centerX(): Int = (left + right) / 2
    fun centerY(): Int = (top + bottom) / 2

    fun center(): IntPoint = IntPoint(centerX(), centerY())

    /** True when this rectangle and [other] share at least one pixel. */
    fun intersects(other: IntRect): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    fun inset(dx: Int, dy: Int): IntRect = IntRect(left + dx, top + dy, right - dx, bottom - dy)

    fun translated(dx: Int, dy: Int): IntRect = IntRect(left + dx, top + dy, right + dx, bottom + dy)
}

/**
 * Request for a single balloon: the center of the touch area it describes, its preferred
 * size and the side it would like its tail to be on.
 */
data class BalloonRequest(
    val id: Int,
    val targetCenter: IntPoint,
    val preferredSize: IntPoint,
    val preferredTailSide: BalloonTailSide,
)

/** A balloon after placement, together with the tail that has to point at the target. */
data class PlacedBalloon(
    val id: Int,
    val bounds: IntRect,
    val tailSide: BalloonTailSide,
    val tailTip: IntPoint,
)

/**
 * Deterministic placement of documentation balloons.
 *
 * Every balloon is first moved next to the center of the view it describes, keeping its
 * preferred tail side. Rectangles are then clamped into the container and, if two of them
 * still overlap, the later one is pushed away from the earlier one along the dominant axis
 * until the result is collision free. The pass is order dependent and therefore fully
 * deterministic, which keeps it testable and avoids balloons jumping around when the
 * overlay is reopened.
 *
 * The class has no Android dependencies on purpose so the packing rules can be unit tested.
 */
class DocBalloonLayout(
    private val containerWidth: Int,
    private val containerHeight: Int,
    private val gap: Int,
    private val tailLength: Int,
    private val edgeMargin: Int,
) {
    /**
     * Places all [requests] and returns the result in the same order.
     *
     * @param obstacles additional rectangles (for example the "Got it!" button) that no
     *   balloon may overlap.
     */
    fun place(requests: List<BalloonRequest>, obstacles: List<IntRect> = emptyList()): List<PlacedBalloon> {
        val reserved = obstacles.map { it.inset(-gap, -gap) }.toMutableList()
        val placed = mutableListOf<PlacedBalloon>()
        for (request in requests) {
            val balloons = placeRequest(request, reserved)
            placed += balloons.second
            reserved += balloons.first.map { it.inset(-gap, -gap) }
        }
        return placed
    }

    private fun placeRequest(
        request: BalloonRequest,
        reserved: List<IntRect>,
    ): Pair<List<IntRect>, PlacedBalloon> {
        val size = clampSize(request.preferredSize)
        val side = chooseTailSide(request)
        val candidates = listOf(side) + BalloonTailSide.entries.filter { it != side }
        for (candidate in candidates) {
            val bounds = clampToContainer(initialBounds(request.targetCenter, size, candidate))
            if (reserved.none { it.intersects(bounds) }) {
                return listOf(bounds) to toPlaced(request, bounds, candidate)
            }
        }

        // Every side is blocked: search a deterministic grid of positions, starting from the
        // preferred one, for the first slot that does not intersect any reserved rectangle.
        val preferred = clampToContainer(initialBounds(request.targetCenter, size, side))
        val free = findFreeSlot(preferred, size, reserved)
        return listOf(free) to toPlaced(request, free, side)
    }

    /**
     * Scans positions top-to-bottom, left-to-right in fixed steps, preferring the given
     * position and its surroundings, and returns the first overlap free slot. Falls back to
     * the preferred position when the container has no room left.
     */
    private fun findFreeSlot(preferred: IntRect, size: IntPoint, reserved: List<IntRect>): IntRect {
        val stepX = (size.x / 2).coerceAtLeast(gap)
        val stepY = (size.y / 2).coerceAtLeast(gap)
        val startX = preferred.left - preferred.left % stepX
        val startY = preferred.top - preferred.top % stepY
        val orderedX = buildList {
            add(edgeMargin)
            add(containerWidth - edgeMargin - size.x)
            add(startX)
            for (i in 1..containerWidth / stepX) {
                add(startX + i * stepX)
                add(startX - i * stepX)
            }
        }
        val orderedY = buildList {
            add(edgeMargin)
            add(containerHeight - edgeMargin - size.y)
            add(startY)
            for (i in 1..containerHeight / stepY) {
                add(startY + i * stepY)
                add(startY - i * stepY)
            }
        }
        for (y in orderedY) {
            for (x in orderedX) {
                val candidate = clampToContainer(IntRect(x, y, x + size.x, y + size.y))
                if (reserved.none { it.intersects(candidate) }) {
                    return candidate
                }
            }
        }
        return preferred
    }

    private fun toPlaced(request: BalloonRequest, bounds: IntRect, side: BalloonTailSide): PlacedBalloon =
        PlacedBalloon(request.id, bounds, side, tailTipFor(request.targetCenter, bounds, side))

    /**
     * Picks the tail side that has the most room around the target, preferring the requested
     * side when it is still plausible for the target's position inside the container.
     */
    private fun chooseTailSide(request: BalloonRequest): BalloonTailSide {
        val size = clampSize(request.preferredSize)
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

    private fun fallbackSide(center: IntPoint, size: IntPoint): BalloonTailSide = when {
        center.y + size.y <= containerHeight - center.y -> BalloonTailSide.TOP
        else -> BalloonTailSide.BOTTOM
    }

    private fun initialBounds(center: IntPoint, size: IntPoint, side: BalloonTailSide): IntRect {
        val left: Int
        val top: Int
        when (side) {
            BalloonTailSide.TOP -> {
                top = center.y + tailLength
                left = center.x - size.x / 2
            }
            BalloonTailSide.BOTTOM -> {
                top = center.y - tailLength - size.y
                left = center.x - size.x / 2
            }
            BalloonTailSide.LEFT -> {
                left = center.x + tailLength
                top = center.y - size.y / 2
            }
            BalloonTailSide.RIGHT -> {
                left = center.x - tailLength - size.x
                top = center.y - size.y / 2
            }
        }
        return IntRect(left, top, left + size.x, top + size.y)
    }

    private fun clampSize(size: IntPoint): IntPoint = IntPoint(
        size.x.coerceIn(MIN_SIZE, (containerWidth - 2 * edgeMargin).coerceAtLeast(MIN_SIZE)),
        size.y.coerceIn(MIN_SIZE, (containerHeight - 2 * edgeMargin).coerceAtLeast(MIN_SIZE)),
    )

    private fun clampToContainer(rect: IntRect): IntRect {
        val maxLeft = (containerWidth - edgeMargin - rect.width).coerceAtLeast(edgeMargin)
        val maxTop = (containerHeight - edgeMargin - rect.height).coerceAtLeast(edgeMargin)
        val left = rect.left.coerceIn(edgeMargin, maxLeft)
        val top = rect.top.coerceIn(edgeMargin, maxTop)
        return IntRect(left, top, left + rect.width, top + rect.height)
    }

    /**
     * Moves [rect] further away from its target along the balloon's own axis, so repeated
     * steps clear balloons that are stacked behind each other.
     */

    private fun tailTipFor(target: IntPoint, bounds: IntRect, side: BalloonTailSide): IntPoint = when (side) {
        BalloonTailSide.TOP -> IntPoint(target.x.coerceIn(bounds.left, bounds.right - 1), target.y)
        BalloonTailSide.BOTTOM -> IntPoint(target.x.coerceIn(bounds.left, bounds.right - 1), target.y)
        BalloonTailSide.LEFT -> IntPoint(target.x, target.y.coerceIn(bounds.top, bounds.bottom - 1))
        BalloonTailSide.RIGHT -> IntPoint(target.x, target.y.coerceIn(bounds.top, bounds.bottom - 1))
    }

    private companion object {
        const val MIN_SIZE = 1
    }
}
