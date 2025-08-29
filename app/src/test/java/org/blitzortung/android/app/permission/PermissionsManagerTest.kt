package org.blitzortung.android.app.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowResources
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.Main
import org.junit.Ignore
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P]) // Target a specific SDK relevant for permissions
class PermissionsManagerTest {

    @MockK
    private lateinit var activity: Activity

    private lateinit var permissionsSupport: PermissionsSupport

    private val PERMISSION_STRING = Manifest.permission.ACCESS_COARSE_LOCATION
    private val REQUEST_CODE = 123
    private val PERMISSION_RATIONALE_STRING_ID = android.R.string.ok // Dummy resource ID

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        permissionsSupport = PermissionsSupport(activity)
    }

    // Tests for PermissionsSupport.requestPermission()

    @Test
    fun `requestPermission when permission already granted should return false`() {
        every {activity.checkSelfPermission(PERMISSION_STRING)} answers { PackageManager.PERMISSION_GRANTED }

        // Act
        val result = permissionsSupport.requestPermission(
            PERMISSION_STRING,
            REQUEST_CODE,
            PERMISSION_RATIONALE_STRING_ID
        )

        assertThat(result).isFalse()
        verify(exactly = 0) {activity.requestPermissions(any(), any())}
    }

    @Test
    fun `requestPermission when not granted and no rationale needed should request directly and return true`() {
        every {activity.checkSelfPermission(PERMISSION_STRING)} answers { PackageManager.PERMISSION_DENIED }
        every {activity.shouldShowRequestPermissionRationale(PERMISSION_STRING)} answers { false }

        val result = permissionsSupport.requestPermission(
            PERMISSION_STRING,
            REQUEST_CODE,
            PERMISSION_RATIONALE_STRING_ID
        )

        assertThat(result).isTrue()
        verify(exactly = 1) {activity.requestPermissions(arrayOf(PERMISSION_STRING), REQUEST_CODE)}
    }

    @Test
    @Ignore("need to refactor this test to use a real Robolectric setup in order to be able to detect the Dialog")
    fun `requestPermission when not granted and rationale needed should show dialog then request and return true`() {
        every {activity.checkSelfPermission(PERMISSION_STRING)} answers { PackageManager.PERMISSION_DENIED }
        every {activity.shouldShowRequestPermissionRationale(PERMISSION_STRING)} answers { true }
        // Arrange
        // Configure the shadow PackageManager to indicate rationale should be shown
        val shadowPackageManager = shadowOf(activity.packageManager)
        shadowPackageManager.setShouldShowRequestPermissionRationale(PERMISSION_STRING, true)

        // Act
        val result = permissionsSupport.requestPermission(
            PERMISSION_STRING,
            REQUEST_CODE,
            android.R.string.copy
        )

        // Assert
        assertThat(result).isTrue() // Indicates a request flow was initiated

        // Check that a dialog was shown
//        val latestDialog = ShadowAlertDialog.getLatestAlertDialog()
        val latestDialog = ShadowDialog.getLatestDialog()
        assertThat(latestDialog).isNotNull()
        assertThat(latestDialog.isShowing).isTrue()
        val shadowDialog = shadowOf(latestDialog)
//        assertThat(shadowDialog.title.toString()).isEqualTo("Test Rationale")

        // Simulate clicking the positive button on the dialog
        latestDialog.findViewById<Button>(android.R.id.button1).performClick()
        assertThat(latestDialog.isShowing).isFalse()
        ShadowLooper.runUiThreadTasks()
//        latestDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

        // Check that permissions were requested after dialog click
    }

    // Mock PermissionRequester for testing ensurePermissions
    class MockPermissionRequester(
        override val name: String,
        private val requestResult: Boolean
    ) : PermissionRequester {
        var requestCalled = false
        override fun request(permissionsSupport: PermissionsSupport): Boolean {
            requestCalled = true
            return requestResult
        }
    }

    // Tests for PermissionsSupport.ensurePermissions()

    @Test
    fun `ensurePermissions with one requester returning true should call request and break`() {
        // Arrange
        val requester = MockPermissionRequester("TestRequester", true)

        // Act
        PermissionsSupport.ensurePermissions(activity, requester)

        // Assert
        assertThat(requester.requestCalled).isTrue()
    }

    @Test
    fun `ensurePermissions with first requester false and second true should call both and break after second`() {
        // Arrange
        val requester1 = MockPermissionRequester("Requester1", false)
        val requester2 = MockPermissionRequester("Requester2", true)

        // Act
        PermissionsSupport.ensurePermissions(activity, requester1, requester2)

        // Assert
        assertThat(requester1.requestCalled).isTrue()
        assertThat(requester2.requestCalled).isTrue()
    }

    @Test
    fun `ensurePermissions with all requesters false should call all`() {
        // Arrange
        val requester1 = MockPermissionRequester("Requester1", false)
        val requester2 = MockPermissionRequester("Requester2", false)

        // Act
        PermissionsSupport.ensurePermissions(activity, requester1, requester2)

        // Assert
        assertThat(requester1.requestCalled).isTrue()
        assertThat(requester2.requestCalled).isTrue()
    }
}
