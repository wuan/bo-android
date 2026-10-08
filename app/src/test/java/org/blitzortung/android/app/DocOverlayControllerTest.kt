package org.blitzortung.android.app

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.content.edit
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DocOverlayControllerTest {
    private lateinit var preferences: SharedPreferences
    private lateinit var parent: FrameLayout
    private lateinit var controller: DocOverlayController

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
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
}
