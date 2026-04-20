package org.blitzortung.android.location.provider

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ManualLocationProviderTest {
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    @Test
    fun noManualLocationWithoutCoordinatesSet() {
        val result = ManualLocationProvider.getManualLocation(preferences)

        assertThat(result).isNull()
    }

    @Test
    fun noManualLocationWithOnlyLongitudeSet() {
        preferences.edit { put(PreferenceKey.LOCATION_LONGITUDE, "11.0") }

        val result = ManualLocationProvider.getManualLocation(preferences)

        assertThat(result).isNull()
    }

    @Test
    fun noManualLocationWithOnlyLatitudeSet() {
        preferences.edit { put(PreferenceKey.LOCATION_LATITUDE, "49.0") }

        val result = ManualLocationProvider.getManualLocation(preferences)

        assertThat(result).isNull()
    }

    @Test
    fun manualLocationSet() {
        preferences.edit {
            put(PreferenceKey.LOCATION_LONGITUDE, "11.0")
            put(PreferenceKey.LOCATION_LATITUDE, "49.0")
        }

        val result = ManualLocationProvider.getManualLocation(preferences)

        assertThat(result).isNotNull()
        assertThat(result?.provider).isEqualTo("")
        assertThat(result?.longitude).isEqualTo(11.0)
        assertThat(result?.latitude).isEqualTo(49.0)
    }
}
