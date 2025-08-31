package org.blitzortung.android.app.permission.requester


import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionsSupport
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class NotificationPermissionRequesterTest {

    @MockK
    private lateinit var permissionsSupport: PermissionsSupport

    private lateinit var notificationPermissionRequester: NotificationPermissionRequester

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        // Create the class under test
        notificationPermissionRequester = NotificationPermissionRequester()
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
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU]) // Test on SDK < TIRAMISU (33)
    fun `request should return the true result of the requestPermission method if SDK is above TIRAMISU`() {
        every { permissionsSupport.request(any(), any(), any()) } answers { true }

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
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU]) // Test on SDK < TIRAMISU (33)
    fun `request should return the false result of the requestPermission method if SDK is above TIRAMISU`() {
        every { permissionsSupport.request(any(), any(), any()) } answers { false }

        val result = notificationPermissionRequester.request(permissionsSupport)

        assertThat(result).isFalse()
        verify(exactly = 1) {
            permissionsSupport.request(
                POST_NOTIFICATIONS,
                101,
                R.string.post_notifications_request
            )
        }
    }
}
