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
 * Horizontal anchoring of a balloon inside the container.
 *
 * [CENTERED_ON_TARGET] keeps the balloon centered over the center of the view it describes.
 * [LEFT_EDGE] and [RIGHT_EDGE] pin the balloon to the corresponding border, so explanations
 * for edge aligned controls sit flush against it.
 */
enum class BalloonHorizontalAlignment {
    LEFT_EDGE,
    CENTERED_ON_TARGET,
    RIGHT_EDGE,
}

/**
 * Which fixed band of the overlay a balloon belongs to.
 *
 * [TOP] balloons form the upper row (status / controls / legend) and are aligned to the same
 * top offset. [BOTTOM] balloons form the lower stack and are laid out from the container
 * bottom upwards. [FLOATING] balloons are placed next to their target.
 */
enum class BalloonRow {
    FLOATING,
    TOP,
    BOTTOM,
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
 * @param row fixed band the balloon belongs to, see [BalloonRow].
 * @param order tie breaker inside a row; for [BalloonRow.BOTTOM] lower values sit closer to the
 *   bottom edge, for [BalloonRow.TOP] lower values are placed first from the left.
 */
data class BalloonRequest(
    val id: Int,
    val targetCenter: IntPoint,
    val preferredSize: IntPoint,
    val preferredTailSide: BalloonTailSide,
    val horizontalAlignment: BalloonHorizontalAlignment = BalloonHorizontalAlignment.CENTERED_ON_TARGET,
    val row: BalloonRow = BalloonRow.FLOATING,
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
 * Balloons are placed in three passes so the requested structure survives overlap resolution:
 *
 * 1. [BalloonRow.TOP] balloons share the upper row and are anchored to the top edge in [order].
 * 2. [BalloonRow.BOTTOM] balloons are laid out from the container bottom upwards in [order], so
 *    the requested bottom-to-top order is preserved.
 * 3. [BalloonRow.FLOATING] balloons are placed next to their target.
 *
 * Inside a row the [BalloonHorizontalAlignment] decides the x position; the alignment is a fixed
 * anchor, so a balloon does not drift away from its edge when it has to avoid an overlap - it
 * only moves along the row (vertically for the top row, upwards for the bottom stack and for
 * edge-aligned floating balloons).
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

        placeRow(requests, BalloonRow.TOP, reserved, placed)
        placeRow(requests, BalloonRow.BOTTOM, reserved, placed)
        placeRow(requests, BalloonRow.FLOATING, reserved, placed)

        return requests.mapNotNull { placed[it.id] }
    }

    private fun placeRow(
        requests: List<BalloonRequest>,
        row: BalloonRow,
        reserved: MutableList<IntRect>,
        placed: MutableMap<Int, PlacedBalloon>,
    ) {
        val ordered = requests.filter { it.row == row }.sortedBy { it.order }
        var previousBottom: Int? = null
        var previousTop: Int? = null
        for (request in ordered) {
            val bounds = when (row) {
                BalloonRow.TOP -> placeTopRowBalloon(request, previousBottom, reserved)
                BalloonRow.BOTTOM -> placeBottomStackBalloon(request, previousTop, reserved)
                BalloonRow.FLOATING -> placeFloatingBalloon(request, reserved)
            }
            placed[request.id] = toPlaced(request, bounds)
            reserved += bounds.inset(-gap, -gap)
            when (row) {
                BalloonRow.TOP -> previousBottom = bounds.bottom
                BalloonRow.BOTTOM -> previousTop = bounds.top
                BalloonRow.FLOATING -> Unit
            }
        }
    }

    /**
     * Top-row balloons are stacked downwards from the top edge in order; each one is anchored
     * directly below the previously placed balloon so they cannot overlap. The horizontal
     * alignment is kept fixed and the balloon only slides horizontally when it has to.
     */
    private fun placeTopRowBalloon(
        request: BalloonRequest,
        previousBottom: Int?,
        reserved: List<IntRect>,
    ): IntRect {
        val size = geometry.clampSize(request.preferredSize)
        val box = geometry.boxSize(size, request.preferredTailSide)
        val top = if (previousBottom == null) edgeMargin else previousBottom + gap
        val left = geometry.alignedLeft(request, box.x)
        val preferred = geometry.clampToContainer(IntRect(left, top, left + box.x, top + box.y))
        val step = (box.x / 2).coerceAtLeast(gap)
        val orderedLeft = buildList {
            add(preferred.left)
            for (i in 1..containerWidth / step) {
                add(preferred.left + i * step)
                add(preferred.left - i * step)
            }
        }
        for (candidateLeft in orderedLeft) {
            val candidate = geometry.clampToContainer(IntRect(candidateLeft, top, candidateLeft + box.x, top + box.y))
            if (reserved.none { it.intersects(candidate) }) {
                return candidate
            }
        }
        return preferred
    }

    /**
     * Bottom-stack balloons ascend from the bottom edge in order, keeping their x anchor.
     * Each balloon is anchored directly above the previously placed one so that differing
     * balloon heights cannot make them overlap.
     */
    private fun placeBottomStackBalloon(
        request: BalloonRequest,
        previousTop: Int?,
        reserved: List<IntRect>,
    ): IntRect {
        val size = geometry.clampSize(request.preferredSize)
        val box = geometry.boxSize(size, request.preferredTailSide)
        val preferred = geometry.clampToContainer(bottomStackBounds(request, box, previousTop))
        val step = (box.y / 2).coerceAtLeast(gap)
        val orderedTop = buildList {
            add(preferred.top)
            for (i in 1..containerHeight / step) {
                add(preferred.top - i * step)
            }
        }
        for (top in orderedTop) {
            val candidate = geometry.clampToContainer(IntRect(preferred.left, top, preferred.left + box.x, top + box.y))
            if (reserved.none { it.intersects(candidate) }) {
                return candidate
            }
        }
        return preferred
    }

    /**
     * Computes the anchor rectangle of a bottom-stack balloon. Without [previousTop] the balloon
     * sits on the bottom edge, otherwise directly above the balloon placed before it.
     */
    private fun bottomStackBounds(request: BalloonRequest, box: IntPoint, previousTop: Int?): IntRect {
        val bandBottom = if (previousTop == null) containerHeight - edgeMargin else previousTop - gap
        val left = geometry.alignedLeft(request, box.x)
        val top = bandBottom - box.y
        return IntRect(left, top, left + box.x, top + box.y)
    }

    private fun placeFloatingBalloon(request: BalloonRequest, reserved: List<IntRect>): IntRect {
        val size = geometry.clampSize(request.preferredSize)
        return when (request.horizontalAlignment) {
            BalloonHorizontalAlignment.RIGHT_EDGE -> placeEdgeAlignedBalloon(request, size, rightEdge = true, reserved)
            BalloonHorizontalAlignment.LEFT_EDGE -> placeEdgeAlignedBalloon(request, size, rightEdge = false, reserved)
            BalloonHorizontalAlignment.CENTERED_ON_TARGET -> placeCenteredBalloon(request, size, reserved)
        }
    }

    private fun placeCenteredBalloon(request: BalloonRequest, size: IntPoint, reserved: List<IntRect>): IntRect {
        val side = geometry.chooseTailSide(request, size)
        val candidates = listOf(side) + BalloonTailSide.entries.filter { it != side }
        return candidates.firstNotNullOfOrNull { candidate ->
            geometry.clampToContainer(floatingBounds(request, size, candidate))
                .takeIf { reserved.none { rect -> rect.intersects(it) } }
        } ?: findFreeSlot(
            geometry.clampToContainer(floatingBounds(request, size, side)),
            geometry.boxSize(size, side),
            reserved,
        )
    }

    /**
     * Edge aligned balloons keep their horizontal anchor fixed and only slide vertically, so the
     * explanation stays flush against the border even when it has to dodge an obstacle.
     */
    private fun placeEdgeAlignedBalloon(
        request: BalloonRequest,
        size: IntPoint,
        rightEdge: Boolean,
        reserved: List<IntRect>,
    ): IntRect {
        val tailSide = if (rightEdge) BalloonTailSide.LEFT else BalloonTailSide.RIGHT
        val box = geometry.boxSize(size, tailSide)
        val left = if (rightEdge) containerWidth - edgeMargin - box.x else edgeMargin
        val preferred = geometry.clampToContainer(floatingBounds(request, size, tailSide))
        val step = (box.y / 2).coerceAtLeast(gap)
        val orderedTop = buildList {
            add(preferred.top)
            for (i in 1..containerHeight / step) {
                add(preferred.top + i * step)
                add(preferred.top - i * step)
            }
        }
        for (top in orderedTop) {
            val candidate = geometry.clampToContainer(IntRect(left, top, left + box.x, top + box.y))
            if (reserved.none { it.intersects(candidate) }) {
                return candidate
            }
        }
        return IntRect(left, preferred.top, left + box.x, preferred.top + box.y)
    }

    private fun floatingBounds(request: BalloonRequest, size: IntPoint, side: BalloonTailSide): IntRect {
        val center = request.targetCenter
        val box = geometry.boxSize(size, side)
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
            for (i in 1..containerHeight / stepY) {
                add(preferred.top + i * stepY)
                add(preferred.top - i * stepY)
            }
        }
        for (y in orderedY) {
            for (x in orderedX) {
                val candidate = geometry.clampToContainer(IntRect(x, y, x + size.x, y + size.y))
                if (reserved.none { it.intersects(candidate) }) {
                    return candidate
                }
            }
        }
        return preferred
    }

    private fun toPlaced(request: BalloonRequest, bounds: IntRect): PlacedBalloon {
        val side = tailSideFor(request)
        return PlacedBalloon(
            request.id,
            bounds,
            side,
            geometry.tailTipFor(request.targetCenter, bounds, side, request.horizontalAlignment),
        )
    }

    private fun tailSideFor(request: BalloonRequest): BalloonTailSide = when {
        // Edge aligned floating balloons point at the target from the opposite border.
        request.row == BalloonRow.FLOATING && request.horizontalAlignment == BalloonHorizontalAlignment.RIGHT_EDGE ->
            BalloonTailSide.LEFT
        request.row == BalloonRow.FLOATING && request.horizontalAlignment == BalloonHorizontalAlignment.LEFT_EDGE ->
            BalloonTailSide.RIGHT
        // Row balloons keep the requested side: top row points up, bottom stack points down.
        request.row == BalloonRow.TOP || request.row == BalloonRow.BOTTOM -> request.preferredTailSide
        else -> geometry.chooseTailSide(request, geometry.clampSize(request.preferredSize))
    }
}
