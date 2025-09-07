package org.blitzortung.android.app.permission.requester


import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import org.blitzortung.android.app.view.get
import org.blitzortung.android.app.view.put
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class NotificationPermissionRequesterTest {

    @MockK
    private lateinit var permissionsSupport: PermissionsSupport

    private lateinit var activity: Activity

    private lateinit var preferences: SharedPreferences

    private lateinit var notificationPermissionRequester: NotificationPermissionRequester

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        activity = Robolectric.buildActivity(Main::class.java)
            .setup()
            .get()

        val context = RuntimeEnvironment.getApplication()
        preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        notificationPermissionRequester = NotificationPermissionRequester(activity, preferences)
    }

    @Test
    fun `name should return correct permission name`() {
        assertThat(notificationPermissionRequester.name).isEqualTo("notification")
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S]) // Test on SDK < TIRAMISU (33)
    fun `request should return false if SDK is below TIRAMISU`() {
        val result = notificationPermissionRequester.request(permissionsSupport)
        assertThat(result).isFalse()

        verify(exactly = 0) { permissionsSupport.request(any(), any(), any()) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `request should return false if SDK is TIRAMISU or above and alert is not enabled`() {
        every { permissionsSupport.request(any(), any(), any()) } answers { true }

        val result = notificationPermissionRequester.request(permissionsSupport)

        assertThat(result).isFalse()
        verify(exactly = 0) { permissionsSupport.request(any(), any(), any()) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `request should return the true result of the requestPermission method if SDK is TIRAMISU or above`() {
        every { permissionsSupport.request(any(), any(), any()) } answers { true }
        preferences.edit {
            put(PreferenceKey.ALERT_ENABLED, true)
        }

        val result = notificationPermissionRequester.request(permissionsSupport)

        assertThat(result).isTrue()
        verify(exactly = 1) {
            permissionsSupport.request(
                POST_NOTIFICATIONS,
                101,
                R.string.post_notifications_request
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `request should return true when background queries are enabled and SDK is TIRAMISU or above`() {
        every { permissionsSupport.request(any(), any(), any()) } answers { true }
        preferences.edit {
            put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "300")
        }

        val result = notificationPermissionRequester.request(permissionsSupport)

        assertThat(result).isTrue()
        verify(exactly = 1) {
            permissionsSupport.request(
                POST_NOTIFICATIONS,
                101,
                R.string.post_notifications_request
            )
        }
    }

    @Test
    fun `onRequestPermissionsResult returns false if request code does not match`() {
        val result = notificationPermissionRequester.onRequestPermissionsResult(
            requestCode = 123,
            permissions = arrayOf(POST_NOTIFICATIONS),
            grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED)
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `onRequestPermissionsResult returns false if grant results is empty`() {
        val result = notificationPermissionRequester.onRequestPermissionsResult(
            requestCode = 101,
            permissions = arrayOf(POST_NOTIFICATIONS),
            grantResults = intArrayOf()
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `onRequestPermissionsResult returns true if permission is granted`() {
        preferences.edit {
            put(PreferenceKey.ALERT_ENABLED, true)
            put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "300")
        }

        val result = notificationPermissionRequester.onRequestPermissionsResult(
            requestCode = 101,
            permissions = arrayOf(POST_NOTIFICATIONS),
            grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED)
        )

        assertThat(result).isTrue()

        assertThat(preferences.get(PreferenceKey.ALERT_ENABLED, false)).isTrue()
        assertThat(preferences.get(PreferenceKey.BACKGROUND_QUERY_PERIOD, "0")).isEqualTo("300")
    }

    @Test
    fun `onRequestPermissionsResult returns true if permission is denied and alerts are enabled`() {
        preferences.edit {
            put(PreferenceKey.ALERT_ENABLED, true)
        }

        val result = notificationPermissionRequester.onRequestPermissionsResult(
            requestCode = 101,
            permissions = arrayOf(POST_NOTIFICATIONS),
            grantResults = intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        assertThat(result).isTrue()

        assertThat(preferences.get(PreferenceKey.ALERT_ENABLED, true)).isFalse()
    }

    @Test
    fun `onRequestPermissionsResult returns true if permission is denied and background queries are enabled`() {
        preferences.edit {
            put(PreferenceKey.BACKGROUND_QUERY_PERIOD, "300")
        }

        val result = notificationPermissionRequester.onRequestPermissionsResult(
            requestCode = 101,
            permissions = arrayOf(POST_NOTIFICATIONS),
            grantResults = intArrayOf(PackageManager.PERMISSION_DENIED)
        )

        assertThat(result).isTrue()

        assertThat(preferences.get(PreferenceKey.BACKGROUND_QUERY_PERIOD, "300")).isEqualTo("0")
    }
}
