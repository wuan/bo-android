package org.blitzortung.android.app.permission

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build
import org.blitzortung.android.app.Main.Companion.REQUEST_CODE_POST_NOTIFICATIONS
import org.blitzortung.android.app.R

class Notification(
    private val permissionsHelper: PermissionsHelper,
): PermissionRequester {
    override fun getName(): String = "notification"

    override fun request(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsHelper.requestPermission(POST_NOTIFICATIONS, REQUEST_CODE_POST_NOTIFICATIONS, R.string.post_notifications_request)
        } else {
            false
        }
    }
}
