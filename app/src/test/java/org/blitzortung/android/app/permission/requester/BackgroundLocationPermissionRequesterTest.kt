package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager.PASSIVE_PROVIDER
import android.os.Build
import androidx.core.content.edit
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.settings.getString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BackgroundLocationPermissionRequesterTest {
    @MockK
    private lateinit var permissionsSupport: PermissionsSupport

    private lateinit var activity: Activity

    private lateinit var preferences: SharedPreferences

    private lateinit var backgroundLocationPermissionRequester: BackgroundLocationPermissionRequester

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        activity =
            Robolectric
                .buildActivity(Main::class.java)
                .setup()
                .get()

        val context = RuntimeEnvironment.getApplication()

        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        preferences.edit { put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "300") }
        preferences.edit { put(PreferenceKey.LOCATION_MODE, PASSIVE_PROVIDER) }
        preferences.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, true) }

        backgroundLocationPermissionRequester =
            BackgroundLocationPermissionRequester(activity, preferences)
    }

    @Test
    fun `name should return correct permission name`() {
        assertThat(backgroundLocationPermissionRequester.name).isEqualTo("background location")
    }

    @Test
    fun `request should ignore the permission when background alerts are not enabled`() {
        preferences.edit { put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0") }

        val result = backgroundLocationPermissionRequester.request(permissionsSupport)

        assertThat(result).isFalse()
        verify(exactly = 0) {
            permissionsSupport.request(any(), any(), any())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should not open the dialog if manual location provider is selected`() {
        preferences.edit { put(PreferenceKey.LOCATION_MODE, LocationHandler.MANUAL_PROVIDER) }

        val result = backgroundLocationPermissionRequester.request(permissionsSupport)
        assertThat(result).isFalse()

        verify(exactly = 0) {
            permissionsSupport.request(any(), any(), any())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should request permission directly when disclosure was already shown`() {
        every { permissionsSupport.request(any(), any(), any()) } answers { true }

        val result = backgroundLocationPermissionRequester.request(permissionsSupport)
        assertThat(result).isTrue()

        verify(exactly = 1) {
            permissionsSupport.request(
                ACCESS_BACKGROUND_LOCATION,
                102,
                R.string.location_permission_background_required,
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should not request permission when disclosure has not been shown yet`() {
        preferences.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, false) }

        val result = backgroundLocationPermissionRequester.request(permissionsSupport)
        assertThat(result).isFalse()

        verify(exactly = 0) {
            permissionsSupport.request(any(), any(), any())
        }
    }
}
