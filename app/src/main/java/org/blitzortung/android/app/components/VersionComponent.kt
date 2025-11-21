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
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import javax.inject.Inject
import org.blitzortung.android.app.BuildConfig
import org.blitzortung.android.app.Main

class VersionComponent
    @Inject
    constructor(
        context: Context,
        buildVersion: BuildVersion,
    ) {
        var state: State? = null
            private set

        var configuredVersionCode: Int = 0
            private set

        var configuredMajorVersion: Int = 0
            private set

        var configuredMinorVersion: Int = 0
            private set

        private val preferences: SharedPreferences

        init {
            val packageName = context.packageName
            preferences = context.getSharedPreferences(packageName, Context.MODE_PRIVATE)

            configuredVersionCode = getAndStoreVersion(CONFIGURED_VERSION_CODE, buildVersion.versionCode)
            configuredMajorVersion = getAndStoreVersion(CONFIGURED_MAJOR_VERSION, buildVersion.majorVersion)
            configuredMinorVersion = getAndStoreVersion(CONFIGURED_MINOR_VERSION, buildVersion.minorVersion)

            state =
                when {
                    configuredVersionCode == -1 -> State.FIRST_RUN
                    configuredMajorVersion != buildVersion.majorVersion ||
                        configuredMinorVersion != buildVersion.minorVersion -> State.FIRST_RUN_AFTER_UPDATE

                    else -> State.NO_UPDATE
                }

            Log.d(
                Main.LOG_TAG,
                "updateVersionStatus() name=$packageName, state=$state, versionCode=$configuredVersionCode/${buildVersion.versionCode}, major=$configuredMajorVersion/${buildVersion.majorVersion} minor=$configuredMinorVersion/${buildVersion.minorVersion}",
            )
        }

        private fun getAndStoreVersion(
            prefsKey: String,
            newValue: Int,
        ): Int {
            val knownValue = preferences.getInt(prefsKey, -1)
            preferences.edit { putInt(prefsKey, newValue) }
            return knownValue
        }

        enum class State {
            FIRST_RUN,
            FIRST_RUN_AFTER_UPDATE,
            NO_UPDATE,
        }

        companion object {
            internal const val CONFIGURED_VERSION_CODE = "configured_version_code"
            internal const val CONFIGURED_MAJOR_VERSION = "configured_major_version"
            internal const val CONFIGURED_MINOR_VERSION = "configured_minor_version"
        }
    }

class BuildVersion
    @Inject
    constructor() {
        val versionCode = BuildConfig.VERSION_CODE
        var versionName = BuildConfig.VERSION_NAME
        val majorVersion: Int
        val minorVersion: Int
        val patchVersion: Int

        init {
            val versionComponents = versionName.split(".")
            majorVersion = versionComponents[0].toInt()
            minorVersion = versionComponents[1].split('-')[0].toInt()
            patchVersion = if (versionComponents.size > 2) versionComponents[2].toInt() else -1
        }
    }
