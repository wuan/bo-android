package org.blitzortung.android.app.permission.requester


import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Button
import androidx.core.content.edit
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.Main
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionsHelper
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.settings.getString
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class BackgroundLocationPermissionRequesterTest {

    @MockK
    private lateinit var permissionsHelper: PermissionsHelper

    private lateinit var activity: Activity

    private lateinit var preferences: SharedPreferences

    private lateinit var backgroundLocationPermissionRequester: BackgroundLocationPermissionRequester

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        activity = Robolectric.buildActivity(Main::class.java)
            .setup()
            .get()

        val context = RuntimeEnvironment.getApplication()

        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        preferences.edit { put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "300") }

        backgroundLocationPermissionRequester =
            BackgroundLocationPermissionRequester(permissionsHelper, activity, preferences, true)
    }

    @Test
    fun `name should return correct permission name`() {
        assertThat(backgroundLocationPermissionRequester.name).isEqualTo("background location")
    }

    @Test
    fun `request should ignore the permission when background alerts are not enabled`() {
        backgroundLocationPermissionRequester =
            BackgroundLocationPermissionRequester(permissionsHelper, activity, preferences, false)
        every { permissionsHelper.requestPermission(any(), any(), any()) } answers { true }

        val result = backgroundLocationPermissionRequester.request()

        assertThat(result).isFalse()
        verify(exactly = 0) {
            permissionsHelper.requestPermission(any(), any(), any())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should open the dialog if all conditions are valid and request permission on clicking OK`() {
        every { permissionsHelper.requestPermission(any(), any(), any()) } answers { true }

        val result = backgroundLocationPermissionRequester.request()
        assertThat(result).isTrue()

        val dialog = ShadowDialog.getLatestDialog()
        assertThat(dialog.isShowing).isTrue()

        dialog.findViewById<Button>(android.R.id.button1).performClick()
        ShadowLooper.runUiThreadTasks()
        assertThat(dialog.isShowing).isFalse()

        verify(exactly = 1) {
            permissionsHelper.requestPermission(ACCESS_BACKGROUND_LOCATION, 102, R.string.location_permission_background_required)
        }
        assertThat(preferences.getString(PreferenceKey.BACKGROUND_QUERY_PERIOD, "")).isEqualTo("300")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `request should open the dialog if all conditions are valid and request permission on clicking Cancel`() {
        every { permissionsHelper.requestPermission(any(), any(), any()) } answers { true }

        val result = backgroundLocationPermissionRequester.request()
        assertThat(result).isTrue()

        val dialog = ShadowDialog.getLatestDialog()
        assertThat(dialog.isShowing).isTrue()

        dialog.findViewById<Button>(android.R.id.button2).performClick()
        ShadowLooper.runUiThreadTasks()
        assertThat(dialog.isShowing).isFalse()

        verify(exactly = 0) {
            permissionsHelper.requestPermission(any(), any(), any())
        }
        assertThat(preferences.getString(PreferenceKey.BACKGROUND_QUERY_PERIOD, "")).isEqualTo("0")
    }


}
