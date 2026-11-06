package org.blitzortung.android.location

import android.content.SharedPreferences
import android.location.LocationManager
import androidx.preference.PreferenceManager
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.preferences.PreferenceKey
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocationHandlerTest {
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        preferences = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication())
        preferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun `uses the stored location mode by default`() {
        storeLocationMode(LocationManager.NETWORK_PROVIDER)

        val handler = createHandler()

        assertThat(handler.providerType).isEqualTo(LocationManager.NETWORK_PROVIDER)
    }

    @Test
    fun `a runtime override wins over the stored location mode`() {
        storeLocationMode(LocationManager.NETWORK_PROVIDER)
        val handler = createHandler()

        handler.useProvider(LocationManager.GPS_PROVIDER)

        assertThat(handler.providerType).isEqualTo(LocationManager.GPS_PROVIDER)
    }

    @Test
    fun `clearing the override restores the stored location mode`() {
        storeLocationMode(LocationManager.NETWORK_PROVIDER)
        val handler = createHandler()
        handler.useProvider(LocationManager.GPS_PROVIDER)

        handler.clearProviderOverride()

        assertThat(handler.providerType).isEqualTo(LocationManager.NETWORK_PROVIDER)
    }

    @Test
    fun `clearing an absent override leaves the stored provider unchanged`() {
        storeLocationMode(LocationManager.PASSIVE_PROVIDER)
        val handler = createHandler()

        handler.clearProviderOverride()

        assertThat(handler.providerType).isEqualTo(LocationManager.PASSIVE_PROVIDER)
    }

    private fun createHandler() = LocationHandler(RuntimeEnvironment.getApplication(), preferences)

    private fun storeLocationMode(providerName: String) {
        preferences
            .edit()
            .putString(PreferenceKey.LOCATION_MODE.key, providerName)
            .commit()
    }
}
