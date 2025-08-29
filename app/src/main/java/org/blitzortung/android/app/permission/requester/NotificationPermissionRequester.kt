package org.blitzortung.android.app.permission.requester

import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build
import org.blitzortung.android.app.Main.Companion.REQUEST_CODE_POST_NOTIFICATIONS
import org.blitzortung.android.app.R
import org.blitzortung.android.app.permission.PermissionRequester
import org.blitzortung.android.app.permission.PermissionsSupport

class NotificationPermissionRequester() : PermissionRequester {
    override val name: String = "notification"

    override fun request(permissionsSupport: PermissionsSupport): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsSupport.requestPermission(POST_NOTIFICATIONS, REQUEST_CODE_POST_NOTIFICATIONS, R.string.post_notifications_request)
        } else {
            false
        }
    }
}
