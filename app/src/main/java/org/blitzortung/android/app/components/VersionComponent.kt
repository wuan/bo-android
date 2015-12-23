package org.blitzortung.android.app.components

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log

import org.blitzortung.android.app.Main

class VersionComponent(context: Context) {

    var state: State? = null
        private set

    var versionName: String? = null
        private set

    var configuredVersionCode: Int = 0
        private set

    var versionCode: Int = 0
        private set

    init {
        updatePackageInfo(context)
        updateVersionStatus(context)
    }

    private fun updateVersionStatus(context: Context) {
        val packageName = context.packageName
        val preferences = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)
        configuredVersionCode = preferences.getInt(CONFIGURED_VERSION_CODE, -1)

        preferences.edit().putInt(CONFIGURED_VERSION_CODE, versionCode).apply()

        state = when {
            configuredVersionCode == -1 -> State.FIRST_RUN
            configuredVersionCode < versionCode -> State.FIRST_RUN_AFTER_UPDATE
            else -> State.NO_UPDATE
        }

        Log.d(Main.LOG_TAG, "updateVersionStatus() name=$packageName, state=$state, configuredVersion=$configuredVersionCode, currentVersion=$versionCode")
    }

    private fun updatePackageInfo(context: Context) {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)

        versionCode = pInfo.versionCode
        versionName = pInfo.versionName
    }

    enum class State {
        FIRST_RUN, FIRST_RUN_AFTER_UPDATE, NO_UPDATE
    }

    companion object {
        private val CONFIGURED_VERSION_CODE = "configured_version_code"
    }
}
