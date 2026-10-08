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

/**
 * How a balloon wants to be positioned horizontally.
 *
 * [CENTERED_ON_TARGET] keeps the balloon centered over the center of the view it describes.
 * [RIGHT_EDGE] pins the balloon to the right edge of the container, so that explanations for
 * right-aligned controls (for example the button column) sit flush against the border.
 */
enum class BalloonHorizontalAlignment {
    CENTERED_ON_TARGET,
    RIGHT_EDGE,
}

/**
 * Vertical stacking slot for balloons that share the bottom area of the screen.
 *
 * The bottom group is laid out from the container bottom upwards: the balloon with the
 * lowest [BalloonVerticalSlot] is placed first, the next one above it and so on.
 */
enum class BalloonVerticalSlot {
    NONE,
    BOTTOM,
    MIDDLE,
    TOP,
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
 * Request for a single balloon.
 *
 * @param targetCenter center of the view the balloon describes.
 * @param preferredSize size the balloon wants to occupy.
 * @param preferredTailSide preferred tail direction; the layout may flip it if there is no room.
 * @param horizontalAlignment see [BalloonHorizontalAlignment].
 * @param verticalSlot when not [BalloonVerticalSlot.NONE] the balloon joins the bottom stack and
 *   is placed in the corresponding band, ordered from the bottom of the screen upwards.
 * @param order tie breaker inside the bottom stack; lower values are placed closer to the bottom.
 */
data class BalloonRequest(
    val id: Int,
    val targetCenter: IntPoint,
    val preferredSize: IntPoint,
    val preferredTailSide: BalloonTailSide,
    val horizontalAlignment: BalloonHorizontalAlignment = BalloonHorizontalAlignment.CENTERED_ON_TARGET,
    val verticalSlot: BalloonVerticalSlot = BalloonVerticalSlot.NONE,
    val order: Int = id,
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
 * Balloons are placed in two groups:
 *
 * 1. Balloons with a [BalloonVerticalSlot] form the bottom stack and are laid out first, from
 *    the bottom of the container upwards, so that the requested vertical order is preserved.
 * 2. All remaining balloons are placed one by one, either centered on their target or pinned
 *    to the right edge.
 *
 * In both groups a balloon is clamped into the container and, if it would overlap an already
 * placed balloon or an obstacle, a bounded deterministic search finds the next free slot. The
 * result is stable across invocations, which keeps the overlay from jumping around when it is
 * reopened.
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
    private val geometry = BalloonGeometry(containerWidth, containerHeight, tailLength, edgeMargin)

    /**
     * Places all [requests] and returns the result ordered by balloon id.
     *
     * @param obstacles additional rectangles (for example the "Got it!" button) that no
     *   balloon may overlap.
     */
    fun place(requests: List<BalloonRequest>, obstacles: List<IntRect> = emptyList()): List<PlacedBalloon> {
        val reserved = obstacles.map { it.inset(-gap, -gap) }.toMutableList()
        val placed = LinkedHashMap<Int, PlacedBalloon>()

        val bottomStack = requests.filter { it.verticalSlot != BalloonVerticalSlot.NONE }
            .sortedWith(compareBy({ slotRank(it.verticalSlot) }, { it.order }))
        for (request in bottomStack) {
            val bounds = placeBottomStackBalloon(request, reserved)
            placed[request.id] = toPlaced(request, bounds)
            reserved += bounds.inset(-gap, -gap)
        }

        for (request in requests.filter { it.verticalSlot == BalloonVerticalSlot.NONE }) {
            val bounds = placeFloatingBalloon(request, reserved)
            placed[request.id] = toPlaced(request, bounds)
            reserved += bounds.inset(-gap, -gap)
        }

        return requests.mapNotNull { placed[it.id] }
    }

    /** Bottom-stack balloons are anchored to the requested band and may not overlap the stack. */
    private fun placeBottomStackBalloon(request: BalloonRequest, reserved: List<IntRect>): IntRect {
        val size = clampSize(request.preferredSize)
        val box = boxSize(size, BalloonTailSide.TOP)
        val preferred = clampToContainer(bottomStackBounds(request, box))
        return findFreeSlot(preferred, box, reserved, allowTopScan = false)
    }

    /**
     * Computes the anchor rectangle of a bottom-stack balloon. The three bands tile the lower
     * part of the container from the bottom upwards; each band is only as tall as needed, so
     * the balloons end up stacked directly on top of one another.
     */
    private fun bottomStackBounds(request: BalloonRequest, box: IntPoint): IntRect {
        val bandBottom = when (request.verticalSlot) {
            BalloonVerticalSlot.BOTTOM -> containerHeight - edgeMargin
            BalloonVerticalSlot.MIDDLE -> containerHeight - edgeMargin - box.y - gap
            else -> containerHeight - edgeMargin - 2 * (box.y + gap)
        }
        val left = (containerWidth - box.x) / 2
        val top = bandBottom - box.y
        return IntRect(left, top, left + box.x, top + box.y)
    }

    private fun placeFloatingBalloon(request: BalloonRequest, reserved: List<IntRect>): IntRect {
        val size = clampSize(request.preferredSize)
        if (request.horizontalAlignment == BalloonHorizontalAlignment.RIGHT_EDGE) {
            return placeRightAlignedBalloon(request, size, reserved)
        }
        val side = geometry.chooseTailSide(request, size)
        val candidates = listOf(side) + BalloonTailSide.entries.filter { it != side }
        return candidates.firstNotNullOfOrNull { candidate ->
            clampToContainer(floatingBounds(request, size, candidate))
                .takeIf { reserved.none { rect -> rect.intersects(it) } }
        } ?: findFreeSlot(
            clampToContainer(floatingBounds(request, size, side)),
            boxSize(size, side),
            reserved,
            allowTopScan = true,
        )
    }

    /**
     * Right-aligned balloons keep their right edge fixed and only slide vertically, so the
     * menu hint always hugs the right border even when it has to dodge the "Got it!" button.
     */
    private fun placeRightAlignedBalloon(request: BalloonRequest, size: IntPoint, reserved: List<IntRect>): IntRect {
        val box = boxSize(size, BalloonTailSide.LEFT)
        val left = containerWidth - edgeMargin - box.x
        val preferred = clampToContainer(floatingBounds(request, size, BalloonTailSide.LEFT))
        val step = (size.y / 2).coerceAtLeast(gap)
        val orderedTop = buildList {
            add(preferred.top)
            for (i in 1..containerHeight / step) {
                add(preferred.top + i * step)
                add(preferred.top - i * step)
            }
        }
        for (top in orderedTop) {
            val candidate = clampToContainer(IntRect(left, top, left + box.x, top + box.y))
            if (reserved.none { it.intersects(candidate) }) {
                return candidate
            }
        }
        return IntRect(left, preferred.top, left + box.x, preferred.top + box.y)
    }

    private fun floatingBounds(request: BalloonRequest, size: IntPoint, side: BalloonTailSide): IntRect {
        val center = request.targetCenter
        if (request.horizontalAlignment == BalloonHorizontalAlignment.RIGHT_EDGE) {
            // Pin to the right edge; the tail always points left at the target.
            val box = boxSize(size, BalloonTailSide.LEFT)
            val left = containerWidth - edgeMargin - box.x
            val top = center.y - box.y / 2
            return IntRect(left, top, left + box.x, top + box.y)
        }
        val box = boxSize(size, side)
        val left: Int
        val top: Int
        when (side) {
            BalloonTailSide.TOP -> {
                top = center.y
                left = center.x - box.x / 2
            }
            BalloonTailSide.BOTTOM -> {
                top = center.y - box.y
                left = center.x - box.x / 2
            }
            BalloonTailSide.LEFT -> {
                left = center.x
                top = center.y - box.y / 2
            }
            BalloonTailSide.RIGHT -> {
                left = center.x - box.x
                top = center.y - box.y / 2
            }
        }
        return IntRect(left, top, left + box.x, top + box.y)
    }

    /**
     * Scans positions in fixed steps, preferring the given position, and returns the first
     * overlap free slot. Falls back to the preferred position when the container is full.
     */
    private fun findFreeSlot(
        preferred: IntRect,
        size: IntPoint,
        reserved: List<IntRect>,
        allowTopScan: Boolean,
    ): IntRect {
        val stepX = (size.x / 2).coerceAtLeast(gap)
        val stepY = (size.y / 2).coerceAtLeast(gap)
        val startX = preferred.left - preferred.left % stepX
        val orderedX = buildList {
            add(containerWidth - edgeMargin - size.x)
            add(edgeMargin)
            add(startX)
            for (i in 1..containerWidth / stepX) {
                add(startX + i * stepX)
                add(startX - i * stepX)
            }
        }
        val orderedY = buildList {
            add(preferred.top)
            if (allowTopScan) {
                for (i in 1..containerHeight / stepY) {
                    add(preferred.top + i * stepY)
                    add(preferred.top - i * stepY)
                }
            } else {
                // Bottom stack stays anchored: only move upwards, keeping the requested order.
                for (i in 1..containerHeight / stepY) {
                    add(preferred.top - i * stepY)
                }
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

    private fun toPlaced(request: BalloonRequest, bounds: IntRect): PlacedBalloon {
        val side = tailSideFor(request)
        return PlacedBalloon(request.id, bounds, side, geometry.tailTipFor(request.targetCenter, bounds, side))
    }

    private fun tailSideFor(request: BalloonRequest): BalloonTailSide = when {
        // Pinned to the right border, so the tail always points left at the controls.
        request.horizontalAlignment == BalloonHorizontalAlignment.RIGHT_EDGE -> BalloonTailSide.LEFT
        // Stacked balloons point down at the controls below them.
        request.verticalSlot != BalloonVerticalSlot.NONE -> BalloonTailSide.TOP
        else -> geometry.chooseTailSide(request, clampSize(request.preferredSize))
    }

    private fun boxSize(size: IntPoint, side: BalloonTailSide): IntPoint = geometry.boxSize(size, side)

    private fun clampSize(size: IntPoint): IntPoint = IntPoint(
        size.x.coerceIn(MIN_SIZE, (containerWidth - 2 * edgeMargin - tailLength).coerceAtLeast(MIN_SIZE)),
        size.y.coerceIn(MIN_SIZE, (containerHeight - 2 * edgeMargin - tailLength).coerceAtLeast(MIN_SIZE)),
    )

    private fun clampToContainer(rect: IntRect): IntRect {
        val maxLeft = (containerWidth - edgeMargin - rect.width).coerceAtLeast(edgeMargin)
        val maxTop = (containerHeight - edgeMargin - rect.height).coerceAtLeast(edgeMargin)
        val left = rect.left.coerceIn(edgeMargin, maxLeft)
        val top = rect.top.coerceIn(edgeMargin, maxTop)
        return IntRect(left, top, left + rect.width, top + rect.height)
    }

    private fun slotRank(slot: BalloonVerticalSlot): Int = when (slot) {
        BalloonVerticalSlot.BOTTOM -> 0
        BalloonVerticalSlot.MIDDLE -> 1
        BalloonVerticalSlot.TOP -> 2
        BalloonVerticalSlot.NONE -> 3
    }

    private companion object {
        const val MIN_SIZE = 1
    }
}
