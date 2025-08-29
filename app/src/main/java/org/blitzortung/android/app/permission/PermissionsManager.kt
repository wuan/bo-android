package org.blitzortung.android.app.permission

import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import org.blitzortung.android.app.Main.Companion.LOG_TAG
import org.blitzortung.android.app.permission.requester.BackgroundLocationPermissionRequester
import org.blitzortung.android.app.permission.requester.LocationPermissionRequester
import org.blitzortung.android.app.permission.requester.NotificationPermissionRequester
import org.blitzortung.android.app.permission.requester.WakeupPermissionRequester
import javax.inject.Inject

class PermissionsManager @Inject constructor() {
    @RequiresApi(Build.VERSION_CODES.M)
    fun ensurePermissions(activity: Activity, preferences: SharedPreferences, backgroundAlertEnabled: Boolean) {

        val helper = PermissionsHelper(activity)

        val permissionRequesters: List<PermissionRequester> = listOf(
            LocationPermissionRequester(helper, preferences),
            NotificationPermissionRequester(helper),
            BackgroundLocationPermissionRequester(helper, activity, preferences),
            WakeupPermissionRequester(activity, preferences)
        )
        for (permissionRequester in permissionRequesters) {
            val result = permissionRequester.request()
            Log.v(LOG_TAG, "Main.onResume() permission ${permissionRequester.name}: result: $result")
            if (result) {
                break
            }
        }
    }
}

class PermissionsHelper(
    private val activity: Activity
) {

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestPermission(permission: String, requestCode: Int, permissionRequiredStringId: Int): Boolean {
        val shouldShowPermissionRationale = activity.shouldShowRequestPermissionRationale(permission)
        val permissionIsGranted = activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        Log.v(
            LOG_TAG,
            "Main.requestPermission() permission: $permission, requestCode: $requestCode, isGranted: $permissionIsGranted, shouldShowRationale: ${!shouldShowPermissionRationale}"
        )

        return if (!permissionIsGranted) {
            if (shouldShowPermissionRationale) {
                requestPermissionsAfterDialog(permissionRequiredStringId, permission, requestCode)
            } else {
                activity.requestPermissions(arrayOf(permission), requestCode)
            }
            true
        } else {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermissionsAfterDialog(
        dialogTextResource: Int,
        permission: String,
        requestCode: Int,
    ) {
        Log.v(
            LOG_TAG,
            "Main.requestPermissionsAfterDialog() permission: $permission, dialogResource: $dialogTextResource, requestCode: $requestCode"
        )

        val locationText = activity.resources.getString(dialogTextResource)
        AlertDialog.Builder(activity).setMessage(locationText).setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                activity.requestPermissions(arrayOf(permission), requestCode)
            }.show()
    }
}

interface PermissionRequester {
    val name: String
    fun request(): Boolean
}
