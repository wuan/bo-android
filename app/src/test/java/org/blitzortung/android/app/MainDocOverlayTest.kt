package org.blitzortung.android.app

import android.content.SharedPreferences
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

    private fun findBalloon(container: ViewGroup, text: String): View? {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            val label = child.findViewById<android.widget.TextView>(R.id.doc_balloon_text)
            if (label?.text?.toString() == text) {
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
