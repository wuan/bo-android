package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.permission.PermissionsSupport
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BackgroundLocationDisclosureRequesterTest {

    @MockK
    private lateinit var permissionsSupport: PermissionsSupport

    private lateinit var activity: Activity
    private lateinit var preferences: SharedPreferences
    private lateinit var requester: BackgroundLocationDisclosureRequester

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        activity = Robolectric.buildActivity(Main::class.java).setup().get()
        val context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        requester = BackgroundLocationDisclosureRequester(activity, preferences)
    }

    @Test
    fun `name should return correct value`() {
        assertThat(requester.name).isEqualTo("background location disclosure")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should show disclosure on Android Q or higher without any permissions granted`() {
        val result = requester.request(permissionsSupport)

        assertThat(result).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should not show disclosure when already shown`() {
        preferences.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, true) }

        val result = requester.request(permissionsSupport)

        assertThat(result).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should not show disclosure when background location already granted`() {
        shadowOf(activity.application).grantPermissions(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION)

        val result = requester.request(permissionsSupport)

        assertThat(result).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `request should not show disclosure below Android Q`() {
        val result = requester.request(permissionsSupport)

        assertThat(result).isFalse()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should show disclosure regardless of background alert setting`() {
        preferences.edit { put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0") }

        val result = requester.request(permissionsSupport)

        assertThat(result).isTrue()
    }
}
