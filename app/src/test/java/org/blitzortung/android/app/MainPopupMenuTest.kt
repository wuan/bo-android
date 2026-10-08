package org.blitzortung.android.app

import android.app.Activity
import android.content.SharedPreferences
import android.view.MenuItem
import android.view.View
import androidx.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.components.BuildVersion
import org.blitzortung.android.app.components.ChangeLogComponent
import org.blitzortung.android.data.MainDataHandler
import org.blitzortung.android.settings.SettingsActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainPopupMenuTest {
    private lateinit var activity: Activity
    private lateinit var preferences: SharedPreferences
    private lateinit var dataHandler: MainDataHandler
    private lateinit var alertHandler: AlertHandler
    private lateinit var buildVersion: BuildVersion
    private lateinit var changeLogComponent: ChangeLogComponent

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        preferences = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication())
        dataHandler = mockk(relaxed = true)
        alertHandler = mockk(relaxed = true)
        buildVersion = mockk(relaxed = true)
        changeLogComponent = mockk(relaxed = true)
    }

    private fun createPopupMenu() =
        MainPopupMenu(
            activity,
            View(activity),
            preferences,
            dataHandler,
            alertHandler,
            buildVersion,
            changeLogComponent,
        )

    private fun menuItem(itemId: Int): MenuItem {
        val item = mockk<MenuItem>()
        every { item.itemId } returns itemId
        return item
    }

    @Test
    fun `doc overlay menu item shows the documentation overlay`() {
        var overlayShown = false
        val popupMenu = createPopupMenu()
        popupMenu.onShowDocOverlay = { overlayShown = true }
        val listener = popupMenu.ClickListener(activity, preferences, dataHandler, alertHandler)

        val handled = listener.onMenuItemClick(menuItem(R.id.menu_doc_overlay))

        assertThat(handled).isTrue()
        assertThat(overlayShown).isTrue()
    }

    @Test
    fun `preferences menu item opens the settings activity`() {
        val popupMenu = createPopupMenu()
        val listener = popupMenu.ClickListener(activity, preferences, dataHandler, alertHandler)

        val handled = listener.onMenuItemClick(menuItem(R.id.menu_preferences))

        assertThat(handled).isTrue()
        val startedIntent = shadowOf(activity).nextStartedActivity
        assertThat(startedIntent.component?.className).isEqualTo(SettingsActivity::class.java.name)
    }

    @Test
    fun `unknown menu item is not handled`() {
        val popupMenu = createPopupMenu()
        val listener = popupMenu.ClickListener(activity, preferences, dataHandler, alertHandler)

        val handled = listener.onMenuItemClick(menuItem(-1))

        assertThat(handled).isFalse()
    }
}
