/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.app.components

import android.content.Context
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
