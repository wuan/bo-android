package org.blitzortung.android.app

import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1080dp-h1920dp-mdpi")
class MainDocOverlayTest {
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        preferences = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication())
        preferences.edit { clear() }
    }

    @Test
    fun `documentation overlay is shown on first start`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()

        assertThat(activity.findViewById<View>(R.id.doc_overlay_root)).isNotNull()
    }

    @Test
    fun `documentation overlay stays hidden after it has been dismissed once`() {
        preferences.edit { putBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, true) }

        val activity = Robolectric.buildActivity(Main::class.java).setup().get()

        assertThat(activity.findViewById<View>(R.id.doc_overlay_root)).isNull()
    }

    @Test
    fun `got it button dismisses the overlay and remembers the dismissal`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()

        activity.findViewById<Button>(R.id.doc_button_got_it).performClick()

        assertThat(activity.findViewById<View>(R.id.doc_overlay_root)).isNull()
        assertThat(preferences.getBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, false)).isTrue()
    }

    @Test
    fun `menu key opens the popup menu providing the quick guide entry`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()

        val handled = activity.onKeyUp(KeyEvent.KEYCODE_MENU, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU))

        assertThat(handled).isTrue()
    }

    @Test
    fun `menu hint balloon is pinned to the right edge of the container`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val menuBalloon = findBalloon(container, activity.getString(R.string.doc_overlay_buttons))
        assertThat(menuBalloon).isNotNull
        assertThat(container.width).isEqualTo(1080)
        assertThat(menuBalloon!!.left + menuBalloon.width).isEqualTo(container.width - 8)
    }

    @Test
    fun `bottom balloons are stacked time slider below histogram below alert`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val timeSlider = findBalloon(container, activity.getString(R.string.doc_overlay_time_slider))
        val histogram = findBalloon(container, activity.getString(R.string.doc_overlay_histogram))
        val alert = findBalloon(container, activity.getString(R.string.doc_overlay_alert))

        assertThat(timeSlider).isNotNull
        assertThat(histogram).isNotNull
        assertThat(alert).isNotNull
        assertThat(histogram!!.top + histogram.height).isLessThanOrEqualTo(timeSlider!!.top)
        assertThat(alert!!.top + alert.height).isLessThanOrEqualTo(histogram.top)
    }

    @Test
    fun `top row balloons keep their horizontal alignments`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val status = findBalloon(container, activity.getString(R.string.doc_overlay_status))!!
        val menu = findBalloon(container, activity.getString(R.string.doc_overlay_buttons))!!
        val legend = findBalloon(container, activity.getString(R.string.doc_overlay_legend))!!

        // legend flush left, menu flush right, status centered between them
        assertThat(legend.left).isEqualTo(8)
        assertThat(menu.left + menu.width).isEqualTo(container.width - 8)
        assertThat(status.left).isGreaterThan(legend.left)
        assertThat(status.left + status.width).isLessThan(menu.left)
    }

    @Test
    fun `bottom balloons keep their horizontal alignments`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val alert = findBalloon(container, activity.getString(R.string.doc_overlay_alert))!!
        val histogram = findBalloon(container, activity.getString(R.string.doc_overlay_histogram))!!
        val timeSlider = findBalloon(container, activity.getString(R.string.doc_overlay_time_slider))!!

        // alert flush left, histogram flush right, time slider centered
        assertThat(alert.left).isEqualTo(8)
        assertThat(histogram.left + histogram.width).isEqualTo(container.width - 8)
        assertThat(timeSlider.left).isGreaterThan(alert.left)
        assertThat(timeSlider.left + timeSlider.width).isLessThan(container.width - 8)
    }

    @Test
    fun `got it button sits between the top and bottom balloon groups`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val button = activity.findViewById<View>(R.id.doc_button_got_it)
        val topBalloons = listOf(
            findBalloon(container, activity.getString(R.string.doc_overlay_status))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_buttons))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_legend))!!,
        )
        val bottomBalloons = listOf(
            findBalloon(container, activity.getString(R.string.doc_overlay_alert))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_histogram))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_time_slider))!!,
        )

        val topGroupBottom = topBalloons.maxOf { it.top + it.height }
        val bottomGroupTop = bottomBalloons.minOf { it.top }

        assertThat(button.top).isGreaterThanOrEqualTo(topGroupBottom)
        assertThat(button.top + button.height).isLessThanOrEqualTo(bottomGroupTop)
    }

    @Test
    fun `got it button does not overlap any balloon`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val button = activity.findViewById<View>(R.id.doc_button_got_it)
        val buttonRect = Rect(button.left, button.top, button.right, button.bottom)

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val balloonRect = Rect(child.left, child.top, child.left + child.width, child.top + child.height)
            assertThat(Rect.intersects(balloonRect, buttonRect)).isFalse()
        }
    }

    @Test
    fun `top row balloons point their tail up and are stacked without overlap`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val topBalloons = listOf(
            findBalloon(container, activity.getString(R.string.doc_overlay_status))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_buttons))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_legend))!!,
        )

        // Tails point up: the tail sits above the body of its balloon.
        topBalloons.forEach { balloon ->
            val tail = balloon.findViewById<View>(R.id.doc_balloon_tail)
            assertThat(tail.width).isGreaterThan(0)
            assertThat(tail.height).isGreaterThan(0)
            assertThat(tail.top).isLessThan(balloon.findViewById<View>(R.id.doc_balloon_body).top)
        }
        assertNoOverlap(topBalloons)
    }

    @Test
    fun `bottom balloons point their tail down and are stacked without overlap`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val bottomBalloons = listOf(
            findBalloon(container, activity.getString(R.string.doc_overlay_time_slider))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_histogram))!!,
            findBalloon(container, activity.getString(R.string.doc_overlay_alert))!!,
        )

        // Tails point down: the tail sits below the body of its balloon.
        bottomBalloons.forEach { balloon ->
            val tail = balloon.findViewById<View>(R.id.doc_balloon_tail)
            assertThat(tail.width).isGreaterThan(0)
            assertThat(tail.height).isGreaterThan(0)
            assertThat(tail.top).isGreaterThanOrEqualTo(balloon.findViewById<View>(R.id.doc_balloon_body).bottom)
        }
        assertNoOverlap(bottomBalloons)
    }

    @Test
    fun `all balloons are pairwise non-overlapping`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val balloons = (0 until container.childCount).map { container.getChildAt(it) }

        assertThat(balloons).hasSizeGreaterThanOrEqualTo(6)
        assertNoOverlap(balloons)
    }

    @Test
    fun `alert balloon tail sits on the left portion of its edge`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val alert = findBalloon(container, activity.getString(R.string.doc_overlay_alert))!!
        val tail = alert.findViewById<View>(R.id.doc_balloon_tail)
        val body = alert.findViewById<View>(R.id.doc_balloon_body)

        val tailCenterX = tail.left + tail.width / 2
        val bodyCenterX = body.left + body.width / 2
        assertThat(tailCenterX).isLessThan(bodyCenterX)
    }

    @Test
    fun `histogram balloon tail sits on the right portion of its edge`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val histogram = findBalloon(container, activity.getString(R.string.doc_overlay_histogram))!!
        val tail = histogram.findViewById<View>(R.id.doc_balloon_tail)
        val body = histogram.findViewById<View>(R.id.doc_balloon_body)

        val tailCenterX = tail.left + tail.width / 2
        val bodyCenterX = body.left + body.width / 2
        assertThat(tailCenterX).isGreaterThan(bodyCenterX)
    }

    @Test
    fun `legend alert and histogram balloons contain their click hints`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        layoutActivity(activity)

        val container = activity.findViewById<ViewGroup>(R.id.doc_balloon_container)
        val legend = findBalloon(container, activity.getString(R.string.doc_overlay_legend))!!
        val alert = findBalloon(container, activity.getString(R.string.doc_overlay_alert))!!
        val histogram = findBalloon(container, activity.getString(R.string.doc_overlay_histogram))!!

        assertThat(balloonText(legend)).contains(activity.getString(R.string.doc_overlay_hint_legend))
        assertThat(balloonText(alert)).contains(activity.getString(R.string.doc_overlay_hint_alert))
        assertThat(balloonText(histogram)).contains(activity.getString(R.string.doc_overlay_hint_histogram))
    }

    private fun balloonText(balloon: View): String =
        balloon.findViewById<android.widget.TextView>(R.id.doc_balloon_text).text.toString()

    private fun assertNoOverlap(views: List<View>) {
        views.forEachIndexed { i, first ->
            views.drop(i + 1).forEach { second ->
                val a = Rect(first.left, first.top, first.left + first.width, first.top + first.height)
                val b = Rect(second.left, second.top, second.left + second.width, second.top + second.height)
                assertThat(Rect.intersects(a, b))
                    .describedAs("%s must not overlap %s", a, b)
                    .isFalse()
            }
        }
    }

    private fun findBalloon(container: ViewGroup, text: String): View? {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val label = child.findViewById<android.widget.TextView>(R.id.doc_balloon_text)
            if (label?.text?.toString()?.startsWith(text) == true) {
                return child
            }
        }
        return null
    }

    private fun layoutActivity(activity: Main) {
        val root = activity.findViewById<View>(R.id.doc_overlay_root)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 1920)
        shadowOf(Looper.getMainLooper()).idle()
        // Re-run layout so the balloon margins applied by the post() callback are honoured.
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 1920)
    }
}
