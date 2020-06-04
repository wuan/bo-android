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

package org.blitzortung.android.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.KeyEvent
import org.blitzortung.android.app.R
import org.blitzortung.android.app.components.BuildVersion
import org.blitzortung.android.app.components.VersionComponent

class InfoDialog(context: Context, buildVersion: BuildVersion) : AlertDialog(context) {

    init {
        setTitle("" + context.resources.getText(R.string.app_name) + " V" + buildVersion.versionName + " (" + buildVersion.versionCode + ")")
        @SuppressLint("InflateParams") val infoDialogView = layoutInflater.inflate(R.layout.info_dialog, null, false)
        setView(infoDialogView)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
