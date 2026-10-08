package org.blitzortung.android.app.docoverlay

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DocBalloonLayoutTest {
    private val layout = DocBalloonLayout(
        containerWidth = 1000,
        containerHeight = 2000,
        gap = 8,
        tailLength = 10,
        edgeMargin = 8,
    )

    private fun request(
        id: Int,
        x: Int,
        y: Int,
        size: IntPoint = IntPoint(200, 100),
        side: BalloonTailSide = BalloonTailSide.TOP,
    ) = BalloonRequest(id, IntPoint(x, y), size, side)

    @Test
    fun `empty input yields no balloons`() {
        assertThat(layout.place(emptyList())).isEmpty()
    }

    @Test
    fun `results keep the request order and ids`() {
        val requests = listOf(
            request(0, 500, 200),
            request(1, 500, 1000),
        )

        val placed = layout.place(requests)

        assertThat(placed).hasSize(2)
        assertThat(placed.map { it.id }).containsExactly(0, 1)
    }

    @Test
    fun `balloon with top tail is placed below its target`() {
        val placed = layout.place(listOf(request(0, 500, 400, side = BalloonTailSide.TOP))).single()

        assertThat(placed.tailSide).isEqualTo(BalloonTailSide.TOP)
        // The box includes the tail strip, which starts at the target center.
        assertThat(placed.bounds.top).isEqualTo(400)
        assertThat(placed.bounds.bottom).isEqualTo(400 + 10 + 100)
    }

    @Test
    fun `balloon with preferred bottom tail is placed above its target`() {
        val placed = layout.place(listOf(request(0, 500, 1600, side = BalloonTailSide.BOTTOM))).single()

        assertThat(placed.tailSide).isEqualTo(BalloonTailSide.BOTTOM)
        // The box ends at the target center, the tail strip occupying its lower edge.
        assertThat(placed.bounds.bottom).isEqualTo(1600)
    }

    @Test
    fun `tail side flips when there is not enough room on the requested side`() {
        val placed = layout.place(listOf(request(0, 500, 1990, side = BalloonTailSide.TOP))).single()

        assertThat(placed.tailSide).isEqualTo(BalloonTailSide.BOTTOM)
    }

    @Test
    fun `tail tip points at the horizontal center of the target`() {
        val target = IntPoint(500, 400)
        val placed = layout.place(listOf(request(0, target.x, target.y, side = BalloonTailSide.TOP))).single()

        assertThat(placed.tailTip.y).isEqualTo(target.y)
        assertThat(placed.tailTip.x).isBetween(placed.bounds.left, placed.bounds.right - 1)
    }

    @Test
    fun `tail tip is clamped into the balloon when the target is off to the side`() {
        val placed = layout.place(listOf(request(0, 10, 400, side = BalloonTailSide.TOP))).single()

        assertThat(placed.tailTip.x).isBetween(placed.bounds.left, placed.bounds.right - 1)
    }

    @Test
    fun `balloons stay inside the container`() {
        val requests = listOf(
            request(0, 5, 5, side = BalloonTailSide.BOTTOM),
            request(1, 995, 5, side = BalloonTailSide.BOTTOM),
            request(2, 5, 1995, side = BalloonTailSide.TOP),
            request(3, 995, 1995, side = BalloonTailSide.TOP),
        )

        val placed = layout.place(requests)

        placed.forEach { balloon ->
            assertThat(balloon.bounds.left).isGreaterThanOrEqualTo(8)
            assertThat(balloon.bounds.top).isGreaterThanOrEqualTo(8)
            assertThat(balloon.bounds.right).isLessThanOrEqualTo(1000 - 8)
            assertThat(balloon.bounds.bottom).isLessThanOrEqualTo(2000 - 8)
        }
    }

    @Test
    fun `balloons forced onto the same spot do not overlap`() {
        val requests = listOf(
            request(0, 500, 1000, side = BalloonTailSide.TOP),
            request(1, 500, 1000, side = BalloonTailSide.TOP),
            request(2, 500, 1000, side = BalloonTailSide.TOP),
        )

        val placed = layout.place(requests)

        assertThat(placed).hasSize(3)
        assertNoOverlaps(placed.map { it.bounds })
    }

    @Test
    fun `overlapping balloons are separated deterministically`() {
        val requests = listOf(
            request(0, 500, 300, size = IntPoint(400, 400), side = BalloonTailSide.TOP),
            request(1, 520, 320, size = IntPoint(400, 400), side = BalloonTailSide.TOP),
        )

        val first = layout.place(requests)
        val second = layout.place(requests)

        assertThat(first).isEqualTo(second)
        assertNoOverlaps(first.map { it.bounds })
    }

    @Test
    fun `balloons avoid obstacle rectangles such as the got it button`() {
        val obstacle = IntRect(700, 1800, 980, 1950)
        val placed = layout.place(
            listOf(request(0, 800, 1700, size = IntPoint(300, 200), side = BalloonTailSide.TOP)),
            listOf(obstacle),
        )

        placed.forEach { assertThat(it.bounds.intersects(obstacle)).isFalse() }
    }

    @Test
    fun `oversized balloons are clamped to fit the container`() {
        val placed = layout.place(
            listOf(request(0, 500, 1000, size = IntPoint(5000, 5000), side = BalloonTailSide.TOP)),
        ).single()

        assertThat(placed.bounds.width).isLessThanOrEqualTo(1000 - 2 * 8)
        assertThat(placed.bounds.height).isLessThanOrEqualTo(2000 - 2 * 8)
    }

    @Test
    fun `tail tip coordinates stay inside the balloon bounds`() {
        val requests = listOf(
            request(0, 250, 250, side = BalloonTailSide.TOP),
            request(1, 750, 750, side = BalloonTailSide.BOTTOM),
            request(2, 100, 1500, side = BalloonTailSide.LEFT),
            request(3, 900, 1500, side = BalloonTailSide.RIGHT),
        )

        layout.place(requests).forEach { balloon ->
            when (balloon.tailSide) {
                BalloonTailSide.TOP, BalloonTailSide.BOTTOM ->
                    assertThat(balloon.tailTip.x).isBetween(balloon.bounds.left, balloon.bounds.right - 1)
                BalloonTailSide.LEFT, BalloonTailSide.RIGHT ->
                    assertThat(balloon.tailTip.y).isBetween(balloon.bounds.top, balloon.bounds.bottom - 1)
            }
        }
    }

    @Test
    fun `rect intersection and geometry helpers behave as expected`() {
        val rect = IntRect(0, 0, 100, 50)

        assertThat(rect.width).isEqualTo(100)
        assertThat(rect.height).isEqualTo(50)
        assertThat(rect.center()).isEqualTo(IntPoint(50, 25))
        assertThat(rect.centerX()).isEqualTo(50)
        assertThat(rect.centerY()).isEqualTo(25)
        assertThat(rect.intersects(IntRect(90, 40, 200, 200))).isTrue()
        assertThat(rect.intersects(IntRect(100, 0, 200, 50))).isFalse()
        assertThat(rect.inset(10, 5)).isEqualTo(IntRect(10, 5, 90, 45))
        assertThat(rect.translated(5, 7)).isEqualTo(IntRect(5, 7, 105, 57))
    }

    @Test
    fun `stacked right tailed balloons are pushed apart`() {
        val requests = listOf(
            request(0, 200, 1000, side = BalloonTailSide.RIGHT),
            request(1, 210, 1010, side = BalloonTailSide.RIGHT),
        )

        val placed = layout.place(requests)

        assertThat(placed).hasSize(2)
        assertNoOverlaps(placed.map { it.bounds })
    }

    @Test
    fun `stacked left tailed balloons are separated as well`() {
        val requests = listOf(
            request(0, 800, 1000, side = BalloonTailSide.LEFT),
            request(1, 810, 1010, side = BalloonTailSide.LEFT),
        )

        val placed = layout.place(requests)

        assertThat(placed).hasSize(2)
        assertNoOverlaps(placed.map { it.bounds })
    }

    @Test
    fun `many balloons at the same target are all separated`() {
        val requests = (0 until 8).map { request(it, 500, 1000, side = BalloonTailSide.TOP) }

        val placed = layout.place(requests)

        assertThat(placed).hasSize(8)
        assertNoOverlaps(placed.map { it.bounds })
        placed.forEach { balloon ->
            assertThat(balloon.bounds.bottom).isLessThanOrEqualTo(2000 - 8)
        }
    }

    @Test
    fun `large stacked balloons are separated by the grid search`() {
        val requests = listOf(
            request(0, 300, 1000, size = IntPoint(400, 600), side = BalloonTailSide.RIGHT),
            request(1, 310, 1010, size = IntPoint(400, 600), side = BalloonTailSide.RIGHT),
        )

        val placed = layout.place(requests)

        assertThat(placed).hasSize(2)
        assertNoOverlaps(placed.map { it.bounds })
    }

    @Test
    fun `right edge aligned balloon sits flush against the right container edge`() {
        val placed = layout.place(
            listOf(
                BalloonRequest(
                    id = 0,
                    targetCenter = IntPoint(100, 500),
                    preferredSize = IntPoint(200, 100),
                    preferredTailSide = BalloonTailSide.LEFT,
                    horizontalAlignment = BalloonHorizontalAlignment.RIGHT_EDGE,
                ),
            ),
        ).single()

        assertThat(placed.bounds.right).isEqualTo(1000 - 8)
        assertThat(placed.tailSide).isEqualTo(BalloonTailSide.LEFT)
        assertThat(placed.bounds.left).isGreaterThan(100)
    }

    @Test
    fun `right edge aligned balloon keeps pointing left at its target`() {
        val target = IntPoint(120, 400)
        val placed = layout.place(
            listOf(
                BalloonRequest(
                    id = 0,
                    targetCenter = target,
                    preferredSize = IntPoint(300, 120),
                    preferredTailSide = BalloonTailSide.LEFT,
                    horizontalAlignment = BalloonHorizontalAlignment.RIGHT_EDGE,
                ),
            ),
        ).single()

        assertThat(placed.tailSide).isEqualTo(BalloonTailSide.LEFT)
        assertThat(placed.tailTip.x).isEqualTo(target.x)
        assertThat(placed.tailTip.y).isBetween(placed.bounds.top, placed.bounds.bottom - 1)
    }

    @Test
    fun `bottom stack keeps the time slider below histogram below alert order`() {
        val timeSlider = stacked(0, order = 0, y = 1900)
        val histogram = stacked(1, order = 1, y = 1600)
        val alert = stacked(2, order = 2, y = 1400)

        val placed = layout.place(listOf(alert, histogram, timeSlider))
        val byId = placed.associateBy { it.id }

        val timeBounds = byId.getValue(0).bounds
        val histogramBounds = byId.getValue(1).bounds
        val alertBounds = byId.getValue(2).bounds

        assertThat(timeBounds.bottom).isEqualTo(2000 - 8)
        assertThat(histogramBounds.bottom).isLessThanOrEqualTo(timeBounds.top)
        assertThat(alertBounds.bottom).isLessThanOrEqualTo(histogramBounds.top)
        assertNoOverlaps(listOf(timeBounds, histogramBounds, alertBounds))
    }

    @Test
    fun `bottom stack order is independent of the request order`() {
        val timeSlider = stacked(0, order = 0, y = 1900)
        val histogram = stacked(1, order = 1, y = 1600)
        val alert = stacked(2, order = 2, y = 1400)

        val placed = layout.place(listOf(timeSlider, alert, histogram))
        val byId = placed.associateBy { it.id }

        assertThat(byId.getValue(1).bounds.bottom).isLessThanOrEqualTo(byId.getValue(0).bounds.top)
        assertThat(byId.getValue(2).bounds.bottom).isLessThanOrEqualTo(byId.getValue(1).bounds.top)
        assertNoOverlaps(placed.map { it.bounds })
    }

    @Test
    fun `stacked balloons always point their tail downwards`() {
        val placed = layout.place(
            listOf(
                stacked(0, order = 0, y = 1900),
                stacked(1, order = 1, y = 1600),
                stacked(2, order = 2, y = 1400),
            ),
        )

        assertThat(placed).allMatch { it.tailSide == BalloonTailSide.TOP }
    }

    @Test
    fun `bottom stack avoids the got it button obstacle`() {
        val obstacle = IntRect(600, 1800, 980, 1950)

        val placed = layout.place(
            listOf(stacked(0, order = 0, y = 1900, size = IntPoint(300, 120))),
            listOf(obstacle),
        ).single()

        assertThat(placed.bounds.intersects(obstacle)).isFalse()
    }

    private fun stacked(
        id: Int,
        order: Int,
        y: Int,
        size: IntPoint = IntPoint(200, 100),
        alignment: BalloonHorizontalAlignment = BalloonHorizontalAlignment.CENTERED_ON_TARGET,
    ) = BalloonRequest(
        id = id,
        targetCenter = IntPoint(500, y),
        preferredSize = size,
        preferredTailSide = BalloonTailSide.TOP,
        horizontalAlignment = alignment,
        row = BalloonRow.BOTTOM,
        order = order,
    )

    @Test
    fun `top row balloons share the top offset`() {
        val placed = layout.place(
            listOf(
                topRow(0, BalloonHorizontalAlignment.CENTERED_ON_TARGET),
                topRow(1, BalloonHorizontalAlignment.RIGHT_EDGE),
                topRow(2, BalloonHorizontalAlignment.LEFT_EDGE),
            ),
        )

        assertThat(placed.map { it.bounds.top }).containsOnly(8)
        assertNoOverlaps(placed.map { it.bounds })
    }

    @Test
    fun `top row honors left center and right horizontal alignments`() {
        val placed = layout.place(
            listOf(
                topRow(0, BalloonHorizontalAlignment.CENTERED_ON_TARGET, size = IntPoint(200, 100)),
                topRow(1, BalloonHorizontalAlignment.RIGHT_EDGE, size = IntPoint(200, 100)),
                topRow(2, BalloonHorizontalAlignment.LEFT_EDGE, size = IntPoint(200, 100)),
            ),
        )
        val byId = placed.associateBy { it.id }

        // left aligned hugs the left margin
        assertThat(byId.getValue(2).bounds.left).isEqualTo(8)
        // right aligned hugs the right margin
        assertThat(byId.getValue(1).bounds.right).isEqualTo(1000 - 8)
        // centered stays between them
        assertThat(byId.getValue(0).bounds.left).isGreaterThan(byId.getValue(2).bounds.left)
        assertThat(byId.getValue(0).bounds.right).isLessThan(byId.getValue(1).bounds.right)
    }

    @Test
    fun `bottom stack honors the per-balloon horizontal alignments`() {
        val placed = layout.place(
            listOf(
                stacked(0, order = 0, y = 1900, alignment = BalloonHorizontalAlignment.CENTERED_ON_TARGET),
                stacked(1, order = 1, y = 1600, alignment = BalloonHorizontalAlignment.RIGHT_EDGE),
                stacked(2, order = 2, y = 1400, alignment = BalloonHorizontalAlignment.LEFT_EDGE),
            ),
        )
        val byId = placed.associateBy { it.id }

        assertThat(byId.getValue(2).bounds.left).isEqualTo(8)
        assertThat(byId.getValue(1).bounds.right).isEqualTo(1000 - 8)
        assertThat(byId.getValue(0).bounds.left).isGreaterThan(byId.getValue(2).bounds.left)
    }

    @Test
    fun `edge aligned rows keep their anchor even when they must avoid an obstacle`() {
        val obstacle = IntRect(0, 8, 1000, 120)
        val placed = layout.place(
            listOf(
                stacked(0, order = 0, y = 1900, alignment = BalloonHorizontalAlignment.RIGHT_EDGE),
            ),
            listOf(obstacle),
        ).single()

        assertThat(placed.bounds.right).isEqualTo(1000 - 8)
        assertThat(placed.bounds.intersects(obstacle)).isFalse()
    }

    @Test
    fun `got it button centered between the groups is avoided by every balloon`() {
        val button = IntRect(400, 900, 600, 1000)
        val placed = layout.place(
            listOf(
                topRow(0, BalloonHorizontalAlignment.CENTERED_ON_TARGET),
                topRow(1, BalloonHorizontalAlignment.RIGHT_EDGE),
                topRow(2, BalloonHorizontalAlignment.LEFT_EDGE),
                stacked(3, order = 0, y = 1900),
                stacked(4, order = 1, y = 1600),
                stacked(5, order = 2, y = 1400),
            ),
            listOf(button),
        )

        assertThat(placed).hasSize(6)
        placed.forEach {
            assertThat(it.bounds.intersects(button))
                .describedAs("balloon %s must not overlap the button %s", it.bounds, button)
                .isFalse()
        }
        assertNoOverlaps(placed.map { it.bounds })
    }

    @Test
    fun `edge aligned floating balloons stay flush when an obstacle blocks them`() {
        val obstacle = IntRect(600, 400, 1000, 600)
        val placed = layout.place(
            listOf(
                BalloonRequest(
                    id = 0,
                    targetCenter = IntPoint(500, 500),
                    preferredSize = IntPoint(200, 100),
                    preferredTailSide = BalloonTailSide.LEFT,
                    horizontalAlignment = BalloonHorizontalAlignment.RIGHT_EDGE,
                ),
            ),
            listOf(obstacle),
        ).single()

        assertThat(placed.bounds.right).isEqualTo(1000 - 8)
        assertThat(placed.bounds.intersects(obstacle)).isFalse()
    }

    private fun topRow(
        id: Int,
        alignment: BalloonHorizontalAlignment,
        size: IntPoint = IntPoint(200, 100),
    ) = BalloonRequest(
        id = id,
        targetCenter = IntPoint(500, 100),
        preferredSize = size,
        preferredTailSide = BalloonTailSide.TOP,
        horizontalAlignment = alignment,
        row = BalloonRow.TOP,
        order = id,
    )

    private fun assertNoOverlaps(rects: List<IntRect>) {
        rects.forEachIndexed { i, first ->
            rects.drop(i + 1).forEach { second ->
                assertThat(first.intersects(second))
                    .describedAs("rect %s must not overlap %s", first, second)
                    .isFalse()
            }
        }
    }
}
