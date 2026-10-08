package org.blitzortung.android.app

import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.View
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
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
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
}
