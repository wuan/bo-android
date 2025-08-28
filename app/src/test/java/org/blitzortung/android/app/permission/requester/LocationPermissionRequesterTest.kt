package org.blitzortung.android.app.permission.requester


import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager.NETWORK_PROVIDER
import android.os.Build
import androidx.core.content.edit
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.LocationProviderRelation
import org.blitzortung.android.app.permission.PermissionsHelper
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.location.LocationHandler.Companion.MANUAL_PROVIDER
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LocationPermissionRequesterTest {

    @MockK
    private lateinit var permissionsHelper: PermissionsHelper

    private lateinit var preferences: SharedPreferences

    private lateinit var locationPermissionRequester: LocationPermissionRequester

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        val context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        locationPermissionRequester = LocationPermissionRequester(permissionsHelper, preferences)
    }

    @Test
    fun `name should return correct permission name`() {
        assertThat(locationPermissionRequester.name).isEqualTo("location")
    }

    @Test
    fun `request should use PASSIVE provider by default`() {
        every { permissionsHelper.requestPermission(any(), any(), any()) } answers { true }

        val result = locationPermissionRequester.request()
        assertThat(result).isTrue()

        verify(exactly = 1) { permissionsHelper.requestPermission(ACCESS_FINE_LOCATION, 1, R.string.location_permission_required ) }
    }

    @Test
    fun `MANUAL location should not trigger a permission dialog`() {
        preferences.edit {
            putString(PreferenceKey.LOCATION_MODE.toString(), MANUAL_PROVIDER)
        }
        every { permissionsHelper.requestPermission(any(), any(), any()) } answers { false }

        val result = locationPermissionRequester.request()
        assertThat(result).isFalse()

        verify(exactly = 0) { permissionsHelper.requestPermission(any(), any(), any()) }
    }

    @Test
    fun `request shold be configured correctly for Network location provider`() {
        preferences.edit {
            putString(PreferenceKey.LOCATION_MODE.toString(), NETWORK_PROVIDER)
        }
        every { permissionsHelper.requestPermission(any(), any(), any()) } answers { true }

        val result = locationPermissionRequester.request()
        assertThat(result).isTrue()

        verify(exactly = 1) { permissionsHelper.requestPermission(ACCESS_COARSE_LOCATION, 2, R.string.location_permission_required ) }
    }


}
