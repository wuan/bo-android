package org.blitzortung.android.app.permission

import android.Manifest
import android.R
import android.app.Activity
import android.os.Build
import android.widget.Button
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P]) // Target a specific SDK relevant for permissions
class PermissionsManagerTest {

    private class DummyActivity : Activity()

    private lateinit var activity: Activity
    private lateinit var shadowActivity: ShadowActivity
    private lateinit var shadowPackageManager: ShadowPackageManager

    private lateinit var permissionsSupport: PermissionsSupport

    private val PERMISSION_STRING = Manifest.permission.ACCESS_COARSE_LOCATION
    private val REQUEST_CODE = 123
    private val PERMISSION_RATIONALE_STRING_ID = R.string.ok // Dummy resource ID

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(DummyActivity::class.java).create().get()
        shadowActivity = shadowOf(activity)
        shadowPackageManager = shadowOf(activity.packageManager)

        permissionsSupport = PermissionsSupport(activity)
    }

    // Tests for PermissionsSupport.requestPermission()

    @Test
    fun `requestPermission when permission already granted should return false`() {
        shadowActivity.grantPermissions(PERMISSION_STRING)

        // Act
        val result = permissionsSupport.request(
            PERMISSION_STRING,
            REQUEST_CODE,
            PERMISSION_RATIONALE_STRING_ID
        )

        assertThat(result).isFalse()
        assertThat(shadowActivity.lastRequestedPermission).isNull()
    }

    @Test
    fun `requestPermission when not granted and no rationale needed should request directly and return true`() {
        shadowActivity.denyPermissions(PERMISSION_STRING)
        shadowPackageManager.setShouldShowRequestPermissionRationale(PERMISSION_STRING, false)

        val result = permissionsSupport.request(
            PERMISSION_STRING,
            REQUEST_CODE,
            PERMISSION_RATIONALE_STRING_ID
        )

        assertThat(result).isTrue()
        val permissionRequest = shadowActivity.lastRequestedPermission
        assertThat(permissionRequest).isNotNull()
        assertThat(permissionRequest.requestCode).isEqualTo(REQUEST_CODE)
        assertThat(permissionRequest.requestedPermissions).isEqualTo(arrayOf(PERMISSION_STRING))
    }

    @Test
    fun `requestPermission when not granted and rationale needed should show dialog then request and return true`() {
        shadowActivity.denyPermissions(PERMISSION_STRING)
        shadowPackageManager.setShouldShowRequestPermissionRationale(PERMISSION_STRING, true)

        // Act
        val result = permissionsSupport.request(
            PERMISSION_STRING,
            REQUEST_CODE,
            R.string.copy
        )

        // Assert
        assertThat(result).isTrue() // Indicates a request flow was initiated

        // Check that a dialog was shown
        val latestDialog = ShadowDialog.getLatestDialog()
        assertThat(latestDialog).isNotNull()
        assertThat(latestDialog.isShowing).isTrue()

        // Simulate clicking the positive button on the dialog
        latestDialog.findViewById<Button>(R.id.button1).performClick()
        ShadowLooper.idleMainLooper()
        assertThat(latestDialog.isShowing).isFalse()

        val permissionRequest = shadowActivity.lastRequestedPermission
        assertThat(permissionRequest).isNotNull()
        assertThat(permissionRequest.requestCode).isEqualTo(REQUEST_CODE)
        assertThat(permissionRequest.requestedPermissions).isEqualTo(arrayOf(PERMISSION_STRING))
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
        permissionsSupport = PermissionsSupport(activity)

        // Arrange
        val requester = MockPermissionRequester("TestRequester", true)

        // Act
        PermissionsSupport.ensure(activity, requester)

        // Assert
        assertThat(requester.requestCalled).isTrue()
    }

    @Test
    fun `ensurePermissions with first requester false and second true should call both and break after second`() {
        permissionsSupport = PermissionsSupport(activity)

        // Arrange
        val requester1 = MockPermissionRequester("Requester1", false)
        val requester2 = MockPermissionRequester("Requester2", true)

        // Act
        PermissionsSupport.ensure(activity, requester1, requester2)

        // Assert
        assertThat(requester1.requestCalled).isTrue()
        assertThat(requester2.requestCalled).isTrue()
    }

    @Test
    fun `ensurePermissions with all requesters false should call all`() {
        permissionsSupport = PermissionsSupport(activity)

        // Arrange
        val requester1 = MockPermissionRequester("Requester1", false)
        val requester2 = MockPermissionRequester("Requester2", false)

        // Act
        PermissionsSupport.ensure(activity, requester1, requester2)

        // Assert
        assertThat(requester1.requestCalled).isTrue()
        assertThat(requester2.requestCalled).isTrue()
    }
}
