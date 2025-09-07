package org.blitzortung.android.app.permission.requester


import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager.NETWORK_PROVIDER
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
import org.blitzortung.android.location.LocationHandler.Companion.MANUAL_PROVIDER
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocationPermissionRequesterTest {

    @MockK
    private lateinit var permissionsSupport: PermissionsSupport

    private lateinit var activity: Activity

    private lateinit var preferences: SharedPreferences

    private lateinit var locationPermissionRequester: LocationPermissionRequester

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        activity = Robolectric.buildActivity(Main::class.java)
            .setup()
            .get()

        val context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        locationPermissionRequester = LocationPermissionRequester(activity, preferences)
    }

    @Test
    fun `name should return correct permission name`() {
        assertThat(locationPermissionRequester.name).isEqualTo("location")
    }

    @Test
    fun `request should use PASSIVE provider by default`() {
        every { permissionsSupport.request(any(), any(), any()) } answers { true }

        val result = locationPermissionRequester.request(permissionsSupport)
        assertThat(result).isTrue()

        verify(exactly = 1) {
            permissionsSupport.request(
                ACCESS_FINE_LOCATION, 1, R.string.location_permission_required
            )
        }
    }

    @Test
    fun `MANUAL location should not trigger a permission dialog`() {
        preferences.edit {
            putString(PreferenceKey.LOCATION_MODE.toString(), MANUAL_PROVIDER)
        }
        every { permissionsSupport.request(any(), any(), any()) } answers { false }

        val result = locationPermissionRequester.request(permissionsSupport)
        assertThat(result).isFalse()

        verify(exactly = 0) { permissionsSupport.request(any(), any(), any()) }
    }

    @Test
    fun `request shold be configured correctly for Network location provider`() {
        preferences.edit {
            putString(PreferenceKey.LOCATION_MODE.toString(), NETWORK_PROVIDER)
        }
        every { permissionsSupport.request(any(), any(), any()) } answers { true }

        val result = locationPermissionRequester.request(permissionsSupport)
        assertThat(result).isTrue()

        verify(exactly = 1) {
            permissionsSupport.request(
                ACCESS_COARSE_LOCATION, 2, R.string.location_permission_required
            )
        }
    }

}
