package org.blitzortung.android.app.permission

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import org.blitzortung.android.app.Main.Companion.LOG_TAG

class PermissionsSupport(
    private val activity: Activity,
) {
    fun request(
        permission: String,
        requestCode: Int,
        permissionRequiredStringId: Int,
    ): Boolean {
        val showRationale = activity.shouldShowRequestPermissionRationale(permission)
        val isGranted = activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        Log.v(
            LOG_TAG,
            "PermissionsSupport.request() permission: $permission, requestCode: $requestCode, isGranted: $isGranted, showRationale: $showRationale",
        )

        return if (!isGranted) {
            if (showRationale) {
                requestAfterDialog(permissionRequiredStringId, permission, requestCode)
            } else {
                activity.requestPermissions(arrayOf(permission), requestCode)
            }
            true
        } else {
            false
        }
    }

    private fun requestAfterDialog(
        dialogTextResource: Int,
        permission: String,
        requestCode: Int,
    ) {
        Log.v(
            LOG_TAG,
            "PermissionsSupport.requestPermissionsAfterDialog() permission: $permission, dialogResource: $dialogTextResource, requestCode: $requestCode",
        )

        val message = activity.resources.getString(dialogTextResource)
        AlertDialog.Builder(activity).setMessage(message).setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                activity.requestPermissions(arrayOf(permission), requestCode)
            }.show()
    }

    companion object {
        fun ensure(
            activity: Activity,
            vararg permissionRequesters: PermissionRequester,
        ) {
            val permissionsSupport = PermissionsSupport(activity)

            for (permissionRequester in permissionRequesters) {
                val result = permissionRequester.request(permissionsSupport)
                Log.v(LOG_TAG, "PermissionsSupport.ensure() permission ${permissionRequester.name}: result: $result")
                if (result) {
                    break
                }
            }
        }
    }
}

interface PermissionRequester {
    val name: String

    fun request(permissionsSupport: PermissionsSupport): Boolean

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ): Boolean = false
}
