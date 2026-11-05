package org.blitzortung.android.app

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1080dp-h1920dp-mdpi")
class MainFirstStartTest {
    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        PreferenceManager.getDefaultSharedPreferences(context).edit { clear() }
        // VersionComponent keeps its state in the package-named preferences, so clear them too
        // to make sure the activity really is in the FIRST_RUN state.
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE).edit { clear() }
    }

    @Test
    fun `quick settings dialog is not shown on first start`() {
        val activity = Robolectric.buildActivity(Main::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        assertThat(activity.supportFragmentManager.findFragmentByTag("QuickSettingsDialog")).isNull()
    }
}
