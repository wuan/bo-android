package org.blitzortung.android.app.docoverlay

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DocBalloonViewBinderTest {
    private lateinit var context: Context
    private lateinit var container: FrameLayout
    private lateinit var binder: DocBalloonViewBinder

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        container = FrameLayout(context)
        binder = DocBalloonViewBinder(LayoutInflater.from(context))
    }

    @Test
    fun `create inflates a balloon with the given text and adds it to the container`() {
        val binding = binder.create(container, "hello")

        assertThat(container.childCount).isEqualTo(1)
        assertThat(binding.docBalloonText.text.toString()).isEqualTo("hello")
    }

    @Test
    fun `measure produces a non empty size`() {
        val binding = binder.create(container, "some documentation text")

        binder.measure(binding, 400)

        assertThat(binding.root.measuredWidth).isGreaterThan(0)
        assertThat(binding.root.measuredHeight).isGreaterThan(0)
    }

    @Test
    fun `apply insets the balloon body into the bounds on the tail side`() {
        val binding = binder.create(container, "text")
        val bounds = IntRect(20, 30, 220, 140)

        binder.apply(binding, PlacedBalloon(0, bounds, BalloonTailSide.TOP, IntPoint(120, 30)))

        val params = binding.root.layoutParams as FrameLayout.LayoutParams
        assertThat(params.leftMargin).isEqualTo(bounds.left)
        assertThat(params.width).isEqualTo(bounds.width)
        // Top tail: the body starts below the tail strip but never leaves the placed box.
        assertThat(params.topMargin).isEqualTo(bounds.top + DocBalloonViewBinder.TAIL_LENGTH_PX)
        assertThat(params.topMargin + params.height).isEqualTo(bounds.bottom)
    }

    @Test
    fun `apply attaches the tail below the body and points it at the target`() {
        val binding = binder.create(container, "text")
        val bounds = IntRect(0, 0, 200, 100)

        binder.apply(binding, PlacedBalloon(0, bounds, BalloonTailSide.TOP, IntPoint(120, 0)))

        val tail = binding.docBalloonTail
        val params = tail.layoutParams as RelativeLayout.LayoutParams
        assertThat(tail.drawable).isNotNull()
        assertThat(params.leftMargin).isEqualTo(120 - DocBalloonViewBinder.TAIL_WIDTH_PX / 2)
        assertThat(params.topMargin).isEqualTo(-DocBalloonViewBinder.TAIL_LENGTH_PX)
    }

    @Test
    fun `apply places the tail above the body for a bottom tail`() {
        val binding = binder.create(container, "text")
        val bounds = IntRect(0, 200, 200, 300)

        binder.apply(binding, PlacedBalloon(0, bounds, BalloonTailSide.BOTTOM, IntPoint(100, 300)))

        val params = binding.docBalloonTail.layoutParams as RelativeLayout.LayoutParams
        assertThat(params.bottomMargin).isEqualTo(-DocBalloonViewBinder.TAIL_LENGTH_PX)
        assertThat(binding.root.layoutParams).isNotNull()
    }

    @Test
    fun `apply offsets the body upwards for a bottom tail`() {
        val binding = binder.create(container, "text")

        binder.apply(binding, PlacedBalloon(0, IntRect(0, 200, 200, 300), BalloonTailSide.BOTTOM, IntPoint(100, 300)))

        val params = binding.root.layoutParams as FrameLayout.LayoutParams
        assertThat(params.topMargin).isEqualTo(200)
    }

    @Test
    fun `apply positions horizontal tails to the correct side`() {
        val binding = binder.create(container, "text")

        binder.apply(binding, PlacedBalloon(0, IntRect(100, 100, 300, 200), BalloonTailSide.LEFT, IntPoint(100, 150)))

        val leftParams = binding.docBalloonTail.layoutParams as RelativeLayout.LayoutParams
        assertThat(leftParams.leftMargin).isEqualTo(-DocBalloonViewBinder.TAIL_LENGTH_PX)
        val bodyParams = binding.root.layoutParams as FrameLayout.LayoutParams
        // Left tail: the body is inset from the left edge of the placed box.
        assertThat(bodyParams.leftMargin).isEqualTo(100 + DocBalloonViewBinder.TAIL_LENGTH_PX)
        assertThat(bodyParams.leftMargin + bodyParams.width).isEqualTo(300)

        binder.apply(binding, PlacedBalloon(0, IntRect(100, 100, 300, 200), BalloonTailSide.RIGHT, IntPoint(300, 150)))

        val rightParams = binding.docBalloonTail.layoutParams as RelativeLayout.LayoutParams
        assertThat(rightParams.rightMargin).isEqualTo(-DocBalloonViewBinder.TAIL_LENGTH_PX)
    }

    @Test
    fun `tail dimensions are pronounced compared to the balloon body`() {
        // The pointer must be long enough to read clearly while staying narrower than the body.
        assertThat(DocBalloonViewBinder.TAIL_LENGTH_PX).isGreaterThanOrEqualTo(24)
        assertThat(DocBalloonViewBinder.TAIL_WIDTH_PX).isGreaterThanOrEqualTo(20)
        // A pronounced pointer is longer than it is wide.
        assertThat(DocBalloonViewBinder.TAIL_LENGTH_PX)
            .isGreaterThan(DocBalloonViewBinder.TAIL_WIDTH_PX)
    }

    @Test
    fun `tail length is symmetric for vertical and horizontal tails`() {
        val binding = binder.create(container, "text")

        binder.apply(binding, PlacedBalloon(0, IntRect(0, 0, 200, 40), BalloonTailSide.TOP, IntPoint(100, 0)))
        val verticalHeight = (binding.root.layoutParams as FrameLayout.LayoutParams).height

        binder.apply(binding, PlacedBalloon(0, IntRect(0, 0, 220, 40), BalloonTailSide.LEFT, IntPoint(0, 20)))
        val horizontalWidth = (binding.root.layoutParams as FrameLayout.LayoutParams).width

        // In both orientations the body is inset by exactly one tail length on the tail side.
        assertThat(verticalHeight).isEqualTo(40 - DocBalloonViewBinder.TAIL_LENGTH_PX)
        assertThat(horizontalWidth).isEqualTo(220 - DocBalloonViewBinder.TAIL_LENGTH_PX)
    }

    @Test
    fun `tail is an image view with a drawable after apply`() {
        val binding = binder.create(container, "text")

        binder.apply(binding, PlacedBalloon(0, IntRect(0, 0, 100, 100), BalloonTailSide.TOP, IntPoint(50, 0)))

        assertThat(binding.docBalloonTail).isInstanceOf(ImageView::class.java)
        assertThat(binding.root.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun `tail view gets a non zero size matching the tail dimensions`() {
        val binding = binder.create(container, "text")

        binder.apply(binding, PlacedBalloon(0, IntRect(0, 0, 200, 100), BalloonTailSide.TOP, IntPoint(100, 0)))

        val params = binding.docBalloonTail.layoutParams as RelativeLayout.LayoutParams
        assertThat(params.width).isEqualTo(DocBalloonViewBinder.TAIL_WIDTH_PX)
        assertThat(params.height).isEqualTo(DocBalloonViewBinder.TAIL_LENGTH_PX)
    }

    @Test
    fun `tail view is sized correctly for horizontal tails too`() {
        val binding = binder.create(container, "text")

        binder.apply(binding, PlacedBalloon(0, IntRect(0, 0, 200, 100), BalloonTailSide.LEFT, IntPoint(0, 50)))

        val params = binding.docBalloonTail.layoutParams as RelativeLayout.LayoutParams
        assertThat(params.width).isEqualTo(DocBalloonViewBinder.TAIL_LENGTH_PX)
        assertThat(params.height).isEqualTo(DocBalloonViewBinder.TAIL_WIDTH_PX)
    }

    @Test
    fun `balloon root does not clip the tail`() {
        val binding = binder.create(container, "text")

        assertThat(binding.root.clipChildren).isFalse()
    }
}
