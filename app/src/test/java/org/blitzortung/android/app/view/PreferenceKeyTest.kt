package org.blitzortung.android.app.view

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PreferenceKeyTest {

    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    @Test
    fun `wasBackgroundLocationDisclosureShown returns false when preference is not set`() {
        assertThat(preferences.wasBackgroundLocationDisclosureShown()).isFalse()
    }

    @Test
    fun `wasBackgroundLocationDisclosureShown returns false when preference is explicitly false`() {
        preferences.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, false) }

        assertThat(preferences.wasBackgroundLocationDisclosureShown()).isFalse()
    }

    @Test
    fun `wasBackgroundLocationDisclosureShown returns true when preference is set to true`() {
        preferences.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, true) }

        assertThat(preferences.wasBackgroundLocationDisclosureShown()).isTrue()
    }
}
