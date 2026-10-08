package org.blitzortung.android.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.edit
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.docoverlay.BalloonTailSide
import org.blitzortung.android.app.view.PreferenceKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DocOverlayControllerTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var parent: FrameLayout
    private lateinit var controller: DocOverlayController

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        preferences.edit { clear() }
        parent = FrameLayout(context)
        controller = DocOverlayController(parent, LayoutInflater.from(context), preferences)
    }

    @Test
    fun `show attaches the overlay to the parent`() {
        controller.show()

        assertThat(controller.isShowing).isTrue()
        assertThat(parent.childCount).isEqualTo(1)
        assertThat(parent.findViewById<android.view.View>(R.id.doc_overlay_root)).isNotNull()
    }

    @Test
    fun `show does not attach the overlay twice`() {
        controller.show()
        controller.show()

        assertThat(controller.isShowing).isTrue()
        assertThat(parent.childCount).isEqualTo(1)
    }

    @Test
    fun `showIfNotShownBefore shows the overlay on first start`() {
        controller.showIfNotShownBefore()

        assertThat(controller.isShowing).isTrue()
        assertThat(parent.childCount).isEqualTo(1)
    }

    @Test
    fun `showIfNotShownBefore keeps the overlay hidden after a previous dismissal`() {
        preferences.edit { putBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, true) }

        controller.showIfNotShownBefore()

        assertThat(controller.isShowing).isFalse()
        assertThat(parent.childCount).isEqualTo(0)
    }

    @Test
    fun `hasBeenShownBefore is false until the overlay is dismissed`() {
        assertThat(controller.hasBeenShownBefore()).isFalse()

        controller.dismissAndRemember()

        assertThat(controller.hasBeenShownBefore()).isTrue()
    }

    @Test
    fun `dismiss removes the overlay from the parent`() {
        controller.show()

        controller.dismiss()

        assertThat(controller.isShowing).isFalse()
        assertThat(parent.childCount).isEqualTo(0)
        assertThat(preferences.getBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, false)).isFalse()
    }

    @Test
    fun `dismiss without a visible overlay does nothing`() {
        controller.dismiss()

        assertThat(controller.isShowing).isFalse()
        assertThat(parent.childCount).isEqualTo(0)
    }

    @Test
    fun `dismissAndRemember removes the overlay and persists the preference`() {
        controller.show()

        controller.dismissAndRemember()

        assertThat(controller.isShowing).isFalse()
        assertThat(parent.childCount).isEqualTo(0)
        assertThat(preferences.getBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, false)).isTrue()
    }

    @Test
    fun `got it button dismisses the overlay and remembers the dismissal`() {
        controller.show()

        parent.findViewById<Button>(R.id.doc_button_got_it).performClick()

        assertThat(controller.isShowing).isFalse()
        assertThat(parent.childCount).isEqualTo(0)
        assertThat(preferences.getBoolean(PreferenceKey.DOC_OVERLAY_SHOWN.key, false)).isTrue()
    }

    @Test
    fun `overlay can be shown again on demand after a dismissal`() {
        controller.show()
        controller.dismissAndRemember()

        controller.show()

        assertThat(controller.isShowing).isTrue()
        assertThat(parent.childCount).isEqualTo(1)
    }

    @Test
    fun `balloon text is rendered for every visible target`() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val host = FrameLayout(activity)
        activity.setContentView(host)
        val target = View(activity).apply { layoutParams = FrameLayout.LayoutParams(100, 40) }
        host.addView(target)
        val controllerWithTargets = DocOverlayController(
            host,
            LayoutInflater.from(activity),
            preferences,
        ) {
            listOf(DocTarget(target, "status text", BalloonTailSide.TOP))
        }

        controllerWithTargets.show()
        layoutHost(host)
        shadowOf(Looper.getMainLooper()).idle()

        val texts = collectTexts(host.findViewById<FrameLayout>(R.id.doc_balloon_container))
        assertThat(texts).contains("status text")
    }

    @Test
    fun `targets that are not visible do not produce balloons`() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        val host = FrameLayout(activity)
        activity.setContentView(host)
        val target = View(activity)
        target.visibility = View.GONE
        host.addView(target)
        val controllerWithTargets = DocOverlayController(
            host,
            LayoutInflater.from(activity),
            preferences,
        ) {
            listOf(DocTarget(target, "hidden", BalloonTailSide.TOP))
        }

        controllerWithTargets.show()
        layoutHost(host)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(host.findViewById<FrameLayout>(R.id.doc_balloon_container)?.childCount ?: 0).isZero()
    }


    private fun layoutHost(host: View) {
        val root = host.findViewById<View>(R.id.doc_overlay_root)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 1080, 1920)
    }

    private fun collectTexts(view: ViewGroup?): List<String> {
        if (view == null) {
            return emptyList()
        }
        val result = mutableListOf<String>()
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is TextView) {
                result += child.text.toString()
            } else if (child is ViewGroup) {
                result += collectTexts(child)
            }
        }
        return result
    }
}
